package com.zhou.shoehome.payment.mq;

import com.zhou.shoehome.bean.PaymentInfo;
import com.zhou.shoehome.service.IPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Date;
import java.util.Map;

/**
 * @author zhouzh6
 * @date 2020-10-13
 */
@Component
public class PaymentServiceMqListener {

    @Autowired
    IPaymentService paymentService;

    @JmsListener(destination = "PAYMENT_CHECK_QUEUE", containerFactory = "jmsQueueListener")
    public void consumerPaymentCheckResult(MapMessage mapMessage) {
        try {
            String outTradeNo = mapMessage.getString("out_trade_no");
            int count = mapMessage.getInt("count");
            // 调用支付宝接口  获取交易状态
            Map<String, Object> resultMap = paymentService.checkAlipayPayment(outTradeNo);

            if (resultMap != null && !resultMap.isEmpty()) {
                String tradeStatus = (String) resultMap.get("trade_status");

                if ("trade_success".equals(tradeStatus)) {
                    // 支付成功 更新支付 发送订单队列
                    PaymentInfo paymentInfo = new PaymentInfo();
                    paymentInfo.setOrderSn(outTradeNo);
                    paymentInfo.setPaymentStatus("已支付");
                    // 支付宝的交易凭证号
                    paymentInfo.setAlipayTradeNo((String) resultMap.get("trade_no"));
                    // 回调请求字符串
                    paymentInfo.setCallbackContent((String) resultMap.get("call_back_content"));
                    paymentInfo.setCallbackTime(new Date());

                    paymentService.updatePayment(paymentInfo);
                }
                return;
            }
            // 支付失败  继续发送延迟检查任务,计算延迟时间
            if (count > 0) {
                // 继续发送延迟检查任务，计算延迟时间等
                count--;
                paymentService.sendDelayPaymentResultCheckQueue(outTradeNo, count);
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
