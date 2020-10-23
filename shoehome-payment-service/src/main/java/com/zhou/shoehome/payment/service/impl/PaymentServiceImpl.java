package com.zhou.shoehome.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.zhou.shoehome.bean.PaymentInfo;
import com.zhou.shoehome.mq.ActiveMQUtil;
import com.zhou.shoehome.payment.config.AlipayConfig;
import com.zhou.shoehome.payment.mapper.PaymentInfoMapper;
import com.zhou.shoehome.service.IPaymentService;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhouzh6
 * @date 2020-10-12
 */
@Service
public class PaymentServiceImpl implements IPaymentService {

    @Autowired
    AlipayClient alipayClient;

    @Autowired
    PaymentInfoMapper paymentInfoMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public Map<String, Object> checkAlipayPayment(String outTradeNo) {
        Map<String, Object> resultMap = new HashMap<>();
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("out_trade_no", outTradeNo);
        request.setBizContent(JSON.toJSONString(requestMap));
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if (response.isSuccess()) {
            // 有可能交易已创建，调用成功
            resultMap.put("out_trade_no", response.getOutTradeNo());
            resultMap.put("trade_no", response.getTradeNo());
            resultMap.put("trade_status", response.getTradeStatus());
            resultMap.put("call_back_content", response.getMsg());
        } else {
            System.out.println("有可能交易未创建，调用失败");
        }
        return resultMap;
    }

    @Override
    public void sendDelayPaymentResultCheckQueue(String outTradeNo, int count) {
        // FIXME 抽取发送消息方法
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
                Queue paymentSuccessQueue = session.createQueue("PAYMENT_CHECK_QUEUE");
                MessageProducer producer = session.createProducer(paymentSuccessQueue);
                MapMessage mapMessage = new ActiveMQMapMessage();
                mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, 1000 * 10);
                mapMessage.setString("out_trade_no", outTradeNo);
                mapMessage.setInt("count", count);
                producer.send(mapMessage);
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

    @Transactional
    @Override
    public void updatePayment(PaymentInfo paymentInfo) {
        // 幂等性检查
        PaymentInfo paymentInfoParam = new PaymentInfo();
        // 获取支付信息paymentInfo中的订单
        paymentInfoParam.setOrderSn(paymentInfo.getOrderSn());
        PaymentInfo paymentInfoResult = paymentInfoMapper.selectOne(paymentInfoParam);

        // 如果订单的祝福状态是以支付就直接返回
        if ("已支付".equals(paymentInfoResult.getPaymentStatus())) {
            return;
        }

        String orderSn = paymentInfo.getOrderSn();
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderSn", orderSn);

        // FIXME 抽取该方法
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
                paymentInfoMapper.updateByExampleSelective(paymentInfo, example);
                Queue paymentSuccessQueue = session.createQueue("PAYMENT_SUCCESS_QUEUE");
                MessageProducer producer = session.createProducer(paymentSuccessQueue);
                MapMessage mapMessage = new ActiveMQMapMessage();
                mapMessage.setString("out_trade_no", orderSn);
                producer.send(mapMessage);
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
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public String getForm(String param) {
        //创建API对应的request
        AlipayTradePagePayRequest alipayTradePagePayRequest = new AlipayTradePagePayRequest();
        // 回调函数
        alipayTradePagePayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayTradePagePayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

        alipayTradePagePayRequest.setBizContent(param);
        try {
            return alipayClient.pageExecute(alipayTradePagePayRequest).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return null;
    }
}
