package com.zhou.shoehome.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.zhou.shoehome.bean.OmsOrder;
import com.zhou.shoehome.service.IOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

/**
 * @author zhouzh6
 * @date 2020-10-12
 */
@Component
public class OrderServiceMqListener {

    @Autowired
    IOrderService orderService;

    @JmsListener(destination = "PAYMENT_SUCCESS_QUEUE", containerFactory = "jmsQueueListener")
    public void consumerPaymentResult(MapMessage mapMessage) {
        try {
            String outTradeNo = mapMessage.getString("out_trade_no");
            // 更新订单状态
            OmsOrder omsOrder = new OmsOrder();
            omsOrder.setOrderSn(outTradeNo);
            orderService.updateOrder(outTradeNo);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
