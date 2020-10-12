package com.zhou.shoehome.service;

import com.zhou.shoehome.bean.PaymentInfo;

/**
 * @author zhouzh6
 * @date 2020-10-12
 */
public interface IPaymentService {

    String getForm(String param);

    void savePaymentInfo(PaymentInfo paymentInfo);

    void updatePayment(PaymentInfo paymentInfo);
}
