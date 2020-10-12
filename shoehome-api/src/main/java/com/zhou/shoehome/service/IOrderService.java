package com.zhou.shoehome.service;

import com.zhou.shoehome.bean.OmsOrder;

import java.math.BigDecimal;

/**
 * @author zhouzh6
 * @date 2020-10-11
 */
public interface IOrderService {

    String checkTradeCode(String memberId, String tradeCode);

    String getTradeCode(String memberId);

    void saveOrder(OmsOrder omsOrder);

    OmsOrder getOrderByOutTradeNo(String outTradeNo);
}
