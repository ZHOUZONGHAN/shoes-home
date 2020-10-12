package com.zhou.shoehome.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.zhou.shoehome.bean.OmsOrder;
import com.zhou.shoehome.bean.OmsOrderItem;
import com.zhou.shoehome.order.mapper.OmsOrderItemMapper;
import com.zhou.shoehome.order.mapper.OmsOrderMapper;
import com.zhou.shoehome.service.IOrderService;
import com.zhou.shoehome.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author zhouzh6
 * @date 2020-10-11
 */
@Service
public class OrderServiceImpl implements IOrderService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OmsOrderMapper omsOrderMapper;

    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;

    @Transactional
    @Override
    public void saveOrder(OmsOrder omsOrder) {
        // 保存订单
        omsOrderMapper.insertSelective(omsOrder);
        String omsOrderId = omsOrder.getId();

        // 保存订单项
        List<OmsOrderItem> omsOrderItemList = omsOrder.getOmsOrderItems();
        for (OmsOrderItem omsOrderItem : omsOrderItemList) {
            omsOrderItem.setOrderId(omsOrderId);
            omsOrderItemMapper.insertSelective(omsOrderItem);
        }
    }

    @Transactional
    @Override
    public String checkTradeCode(String memberId, String tradeCode) {
        String tradeCodeCacheKey = "user:" + memberId + ":trade";
        Long verifyRes;
        try (Jedis jedis = redisUtil.getJedis()) {
            // 使用lua脚本控制并发
            String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            verifyRes = (Long) jedis.eval(luaScript, Collections.singletonList(tradeCodeCacheKey), Collections.singletonList(tradeCode));
        }
        if (verifyRes == null || verifyRes == 0L) {
            return "fail";
        }
        return "success";
    }

    @Transactional
    @Override
    public String getTradeCode(String memberId) {
        String tradeCodeCacheKey = "user:" + memberId + ":trade";
        String tradeCode = UUID.randomUUID().toString();
        try (Jedis jedis = redisUtil.getJedis()) {
            jedis.setex(tradeCodeCacheKey, 60 * 15, tradeCode);
        }
        return tradeCode;
    }
}
