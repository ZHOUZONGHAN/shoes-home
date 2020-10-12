package com.zhou.shoehome.config;

import com.zhou.shoehome.util.RedisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhouzh6
 */
@Configuration
public class RedisConfig {

    @Value("${redis.host:disabled}")
    private String host;

    @Value("${redis.port:6379}")
    private int port;

    @Value("${redis.database:0}")
    private int database;

    @Bean
    public RedisUtil getRedisUtil() {
        if (host.equals("disabled")) {
            return null;
        }
        RedisUtil redisUtil = new RedisUtil();
        redisUtil.initJedisPool(host, port, database);
        return redisUtil;
    }
}
