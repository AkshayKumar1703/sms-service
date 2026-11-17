package com.example.sms_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

@Configuration
public class JedisConfig {

  @Bean
  public JedisPool jedisPool(
      @Value("${redis.host:localhost}") String host, @Value("${redis.port:6379}") int port) {
    return new JedisPool(host, port);
  }
}
