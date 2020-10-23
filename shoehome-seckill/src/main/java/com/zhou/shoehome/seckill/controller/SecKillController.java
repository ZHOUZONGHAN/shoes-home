package com.zhou.shoehome.seckill.controller;

import com.zhou.shoehome.util.RedisUtil;
import org.redisson.Redisson;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

/**
 * @author zhouzh6
 * @date 2020-10-13
 */
@Controller
public class SecKillController {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    RedissonClient redissonClient;

    @RequestMapping("seckill")
    @ResponseBody
    public String seckill(String skuId) {
        RSemaphore semaphore = redissonClient.getSemaphore("SEC_KILL" + skuId);
        // 尝试抢购
        boolean tryAcquireRes = semaphore.tryAcquire();
        if (tryAcquireRes) {
            // TODO 秒杀成功
        } else {
            // 秒杀失败
        }
        return "1";
    }

    @RequestMapping("kill")
    @ResponseBody
    public String kill(String skuId) {
        try (Jedis jedis = redisUtil.getJedis()) {
            // 开启商品监控
            String skuCacheKey = "skuid:" + skuId;
            jedis.watch(skuCacheKey);
            int stock = Integer.parseInt(jedis.get(skuCacheKey));
            if (stock > 0) {
                Transaction multi = jedis.multi();
                multi.incrBy(skuCacheKey, -1);
                List<Object> execList = multi.exec();
                if (execList != null && execList.size() > 0) {
                    // TODO 秒杀成功
                } else {
                    // 秒杀失败
                }
            }
        }
        return "1";
    }
}
