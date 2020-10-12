package com.zhou.shoehome.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author zhouzh6
 */
public class RedisUtil {

    private JedisPool jedisPool;

    public void initJedisPool(String host, int port, int database) {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        // 最大连接数
        jedisPoolConfig.setMaxTotal(200);
        // 最多维持maxIdle个空闲连接
        jedisPoolConfig.setMaxIdle(300);
        // 连接耗尽时是否阻塞, false报异常, ture阻塞直到超时, 默认true
        jedisPoolConfig.setBlockWhenExhausted(true);
        // 获取连接时的最大等待毫秒数
        jedisPoolConfig.setMaxWaitMillis(10 * 1000);
        // 在获取连接的时候检查有效性
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPool = new JedisPool(jedisPoolConfig, host, port, 20 * 1000);
    }

    public Jedis getJedis() {
        return jedisPool.getResource();
    }
}
