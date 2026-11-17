package com.example.sms_service.service;

import com.example.sms_service.model.Priority;
import com.example.sms_service.model.QueuedSms;
import com.example.sms_service.model.SmsMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.resps.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class SmsQueueService {

    private static final String QUEUE_KEY = "sms:queue";
    private static final String DEAD_KEY = "sms:dead";
    private static final int MAX_ATTEMPTS = 3;

    private final JedisPool jedisPool;
    private final SmsService smsService;
    private final Executor smsExecutor;
    private final ObjectMapper mapper;
    private final MeterRegistry registry;
    public SmsQueueService(JedisPool jedisPool, SmsService smsService,
                           @Qualifier("smsExecutor") Executor smsExecutor,
                           ObjectMapper mapper, MeterRegistry registry) {
        this.jedisPool = jedisPool;
        this.smsService = smsService;
        this.smsExecutor = smsExecutor;
        this.mapper = mapper;
        this.registry = registry;
    }

    public void enqueue(String phone, String message, Priority priority) {
        push(new QueuedSms(UUID.randomUUID().toString(), phone, message, priority, 0));
        log.info("Queued SMS {} priority={}", mask(phone), priority);
    }

    public List<SmsMessage> processQueue(int count) {
        List<QueuedSms> batch = pop(count);   // Redis connection released here

        List<CompletableFuture<SmsMessage>> futures = batch.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> send(item), smsExecutor))
                .toList();

        return futures.stream().map(CompletableFuture::join).toList();
    }

    private SmsMessage send(QueuedSms item) {
        SmsMessage result = smsService.sendSms(item.phoneNumber(), item.message());
        if ("FAILED".equals(result.getStatus())) {
            if (item.attempts() + 1 >= MAX_ATTEMPTS) {
                log.error("Giving up on {} after {} attempts", mask(item.phoneNumber()), MAX_ATTEMPTS);
                deadLetter(item);
            } else {
                push(item.retry());
            }
        }
        return result;
    }

    private List<QueuedSms> pop(int count) {
        List<QueuedSms> items = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            for (int i = 0; i < count; i++) {
                Tuple next = jedis.zpopmin(QUEUE_KEY);
                if (next == null) break;
                try {
                    items.add(mapper.readValue(next.getElement(), QueuedSms.class));
                } catch (JsonProcessingException e) {
                    log.error("Malformed queue entry moved to dead letter: {}", next.getElement());
                    jedis.lpush(DEAD_KEY, next.getElement());
                }
            }
        }
        return items;
    }

    private void push(QueuedSms sms) {
        // lower score = processed first; priority level dominates, time breaks ties
        double score = sms.priority().getLevel() * 1e13 + System.currentTimeMillis();
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.zadd(QUEUE_KEY, score, mapper.writeValueAsString(sms));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private void deadLetter(QueuedSms sms) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.lpush(DEAD_KEY, mapper.writeValueAsString(sms));
        } catch (JsonProcessingException e) {
            log.error("Could not dead-letter {}", sms.id(), e);
        }
    }

    public static String mask(String phone) {
        return phone.length() > 4 ? "****" + phone.substring(phone.length() - 4) : "****";
    }
    @PostConstruct
    void registerMetrics() {
        Gauge.builder("sms.queue.size", this, s -> s.size(QUEUE_KEY)).register(registry);
        Gauge.builder("sms.dead.size", this, s -> s.size(DEAD_KEY)).register(registry);
    }

    private double size(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            return key.equals(QUEUE_KEY) ? jedis.zcard(key) : jedis.llen(key);
        } catch (Exception e) {
            return Double.NaN;
        }
    }
}
