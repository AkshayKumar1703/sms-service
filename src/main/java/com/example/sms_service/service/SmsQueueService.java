package com.example.sms_service.service;

import com.example.sms_service.model.Priority;
import com.example.sms_service.model.SmsMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.resps.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class SmsQueueService {

    private static final String QUEUE_KEY = "sms:queue";

    private final JedisPool jedisPool;
    private final SmsService smsService;
    private final Executor smsExecutor;

    public SmsQueueService(JedisPool jedisPool,
                           SmsService smsService,
                           @Qualifier("smsExecutor") Executor smsExecutor) {
        this.jedisPool = jedisPool;
        this.smsService = smsService;
        this.smsExecutor = smsExecutor;
    }

    // Add a message to the queue
    public void enqueue(String phone, String message, Priority priority) {
        String value = UUID.randomUUID() + "|" + phone + "|" + message;
        // zpopmin takes the LOWEST score first, so a lower priority level is processed first
        double score = priority.getLevel() * 1e13 + System.currentTimeMillis();

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.zadd(QUEUE_KEY, score, value);
        }
        log.info("Queued SMS to {}", phone);
    }

    // Send up to `count` messages. Returns only the messages that were sent successfully.
    public List<SmsMessage> processQueue(int count) {
        List<CompletableFuture<SmsMessage>> futures = new ArrayList<>();

        // Hold the Redis connection only while popping, not while sending
        try (Jedis jedis = jedisPool.getResource()) {
            for (int i = 0; i < count; i++) {
                Tuple next = jedis.zpopmin(QUEUE_KEY);
                if (next == null) {
                    break; // queue is empty
                }

                String element = next.getElement();
                double score = next.getScore();

                String[] parts = element.split("\\|", 3);
                if (parts.length < 3) {
                    log.error("Dropping malformed queue entry: {}", element);
                    continue;
                }
                String phone = parts[1];
                String message = parts[2];

                // Send on a worker thread; on failure, put the message back in the queue
                futures.add(CompletableFuture.supplyAsync(() -> {
                    log.info("Sending sms to {}", phone);
                    return smsService.sendSms(phone, message);
                }, smsExecutor).exceptionally(ex -> {
                    log.error("Failed to send sms to {}, re-queueing", phone, ex);
                    requeue(element, score);
                    return null;
                }));
            }
        } // Redis connection is returned to the pool here

        // Wait for all workers, then keep only successful results
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();
    }

    // Put a failed message back with its original score so it keeps its priority
    private void requeue(String element, double score) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.zadd(QUEUE_KEY, score, element);
        } catch (Exception e) {
            log.error("Could not re-queue message, it is lost: {}", element, e);
        }
    }
}