package com.zhou.shoehome.service;

import com.zhou.shoehome.bean.PaymentInfo;

import java.util.Map;

/**
 * @author zhouzh6
 * @date 2020-10-12
 */
public interface IPaymentService {

    String getForm(String param);

    void savePaymentInfo(PaymentInfo paymentInfo);

    void updatePayment(PaymentInfo paymentInfo);

    void sendDelayPaymentResultCheckQueue(String outTradeNo, int count);

    Map<String, Object> checkAlipayPayment(String outTradeNo);
}
