package com.zhou.shoehome.service;

import com.zhou.shoehome.bean.OmsCartItem;

import java.util.List;

/**
 * @author zhouzh6
 */
public interface ICartService {

    OmsCartItem getCartsByUser(String memberId, String skuId);

    void addCart(OmsCartItem omsCartItemFromDb);

    void updateCart(OmsCartItem omsCartItemFromDb);

    void synCache(String memberId);

    List<OmsCartItem> cartList(String userId);

    void checkCart(OmsCartItem omsCartItem);

    void deleteCartItem(OmsCartItem omsCartItem);
}
