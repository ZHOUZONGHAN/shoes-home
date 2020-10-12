package com.zhou.shoehome.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.zhou.shoehome.bean.PaymentInfo;
import com.zhou.shoehome.payment.config.AlipayConfig;
import com.zhou.shoehome.payment.mapper.PaymentInfoMapper;
import com.zhou.shoehome.service.IPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

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

    @Override
    public void updatePayment(PaymentInfo paymentInfo) {
        String orderSn = paymentInfo.getOrderSn();

        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderSn", orderSn);

        paymentInfoMapper.updateByExampleSelective(paymentInfo, example);
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
