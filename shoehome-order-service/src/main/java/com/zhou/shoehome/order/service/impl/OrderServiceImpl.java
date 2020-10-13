package com.zhou.shoehome.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.zhou.shoehome.bean.OmsOrder;
import com.zhou.shoehome.bean.OmsOrderItem;
import com.zhou.shoehome.mq.ActiveMQUtil;
import com.zhou.shoehome.order.mapper.OmsOrderItemMapper;
import com.zhou.shoehome.order.mapper.OmsOrderMapper;
import com.zhou.shoehome.service.IOrderService;
import com.zhou.shoehome.util.RedisUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
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

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public void updateOrder(String orderSn) {

        // FIXME 将发送消息代码抽取出来
        Connection connection = null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
        } catch (JMSException e) {
            e.printStackTrace();
        }

        if (session != null) {
            try {
                // 更新的同时发送消息
                Queue paymentSuccessQueue = session.createQueue("ORDER_PAY_QUEUE");
                MessageProducer producer = session.createProducer(paymentSuccessQueue);
                TextMessage textMessage = new ActiveMQTextMessage();

                // 查询订单的对象，转化成json字符串，存入ORDER_PAY_QUEUE的消息队列
                OmsOrder omsOrderParam = new OmsOrder();
                omsOrderParam.setOrderSn(orderSn);
                OmsOrder omsOrderResponse = omsOrderMapper.selectOne(omsOrderParam);
                OmsOrderItem omsOrderItemParam = new OmsOrderItem();
                omsOrderItemParam.setOrderSn(omsOrderParam.getOrderSn());
                List<OmsOrderItem> omsOrderItemList = omsOrderItemMapper.select(omsOrderItemParam);
                omsOrderResponse.setOmsOrderItems(omsOrderItemList);

                textMessage.setText(JSON.toJSONString(omsOrderResponse));

                Example example = new Example(OmsOrder.class);
                example.createCriteria().andEqualTo("orderSn", orderSn);
                OmsOrder omsOrder = new OmsOrder();
                omsOrder.setStatus("1");
                omsOrderMapper.updateByExampleSelective(omsOrder, example);
                producer.send(textMessage);
                session.commit();
            } catch (Exception e) {
                try {
                    // 消息回滚
                    session.rollback();
                } catch (JMSException ex) {
                    ex.printStackTrace();
                }
            } finally {
                try {
                    connection.close();
                    session.close();
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public OmsOrder getOrderByOutTradeNo(String outTradeNo) {
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(outTradeNo);
        return omsOrderMapper.selectOne(omsOrder);
    }

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
