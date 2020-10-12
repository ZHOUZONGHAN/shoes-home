package com.zhou.shoehome.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.zhou.shoehome.annotations.LoginRequired;
import com.zhou.shoehome.bean.OmsCartItem;
import com.zhou.shoehome.bean.OmsOrder;
import com.zhou.shoehome.bean.OmsOrderItem;
import com.zhou.shoehome.bean.UmsMemberReceiveAddress;
import com.zhou.shoehome.service.ICartService;
import com.zhou.shoehome.service.IOrderService;
import com.zhou.shoehome.service.ISkuService;
import com.zhou.shoehome.service.IUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author zhouzh6
 */
@Controller
@CrossOrigin
public class OrderController {

    @Reference
    ICartService cartService;

    @Reference
    IUserService userService;

    @Reference
    IOrderService orderService;

    @Reference
    ISkuService skuService;

    @RequestMapping("submitOrder")
    @LoginRequired(loginSuccess = true)
    public String submitOrder(String receiveAddressId, BigDecimal totalAmount, String tradeCode, HttpServletRequest request) {
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        // 验证交易码
        String verifyRes = orderService.checkTradeCode(memberId, tradeCode);

        if ("success".equals(verifyRes)) {
            List<OmsOrderItem> omsOrderItemList = new ArrayList<>();
            OmsOrder omsOrder = new OmsOrder();
            omsOrder.setAutoConfirmDay(15);
            omsOrder.setCreateTime(new Date());
            omsOrder.setMemberId(memberId);
            omsOrder.setMemberUsername(nickname);
            omsOrder.setNote("记得送双鞋带");
            // 订单号
            String outTradeNo = "shoehome";
            outTradeNo = outTradeNo + System.currentTimeMillis();
            omsOrder.setOrderSn(outTradeNo);
            UmsMemberReceiveAddress umsMemberReceiveAddress = userService.getReceiveAddressById(receiveAddressId);
            omsOrder.setReceiverCity(umsMemberReceiveAddress.getCity());
            omsOrder.setReceiverDetailAddress(umsMemberReceiveAddress.getDetailAddress());
            omsOrder.setReceiverName(umsMemberReceiveAddress.getName());
            omsOrder.setReceiverPhone(umsMemberReceiveAddress.getPhoneNumber());
            omsOrder.setReceiverPostCode(umsMemberReceiveAddress.getPostCode());
            omsOrder.setReceiverProvince(umsMemberReceiveAddress.getProvince());
            omsOrder.setReceiverRegion(umsMemberReceiveAddress.getRegion());
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, 1);
            Date receiveTime = calendar.getTime();
            omsOrder.setReceiveTime(receiveTime);
            // 订单来源 0--PC 1--APP
            omsOrder.setSourceType(0);
            // 订单状态 0--待付款 1--待发货 2--已发货 3--已完成 4--已关闭 5--无效订单
            omsOrder.setStatus("0");
            omsOrder.setTotalAmount(totalAmount);

            List<OmsCartItem> omsCartItemList = cartService.cartList(memberId);

            for (OmsCartItem omsCartItem : omsCartItemList) {
                // 根据用户id从缓存或者数据库中获取商品列表
                if ("1".equals(omsCartItem.getIsChecked())) {
                    OmsOrderItem omsOrderItem = new OmsOrderItem();
                    // 验价
                    boolean verifyPriceRes = skuService.checkPrice(omsCartItem.getProductSkuId(), omsCartItem.getPrice());
                    if (!verifyPriceRes) {
                        return "tradeFail";
                    }

                    // TODO 验库存


                    omsOrderItem.setProductPic(omsCartItem.getProductPic());
                    omsOrderItem.setProductName(omsCartItem.getProductName());

                    omsOrderItem.setOrderSn(outTradeNo);
                    omsOrderItem.setProductCategoryId(omsCartItem.getProductCategoryId());
                    omsOrderItem.setProductPrice(omsCartItem.getPrice());
                    omsOrderItem.setRealAmount(omsCartItem.getTotalPrice());
                    omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                    // 条形码
                    omsOrderItem.setProductSkuCode("45821154564");
                    omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());
                    omsOrderItem.setProductId(omsCartItem.getProductId());
                    omsOrderItem.setProductSn("仓库");

                    omsOrderItemList.add(omsOrderItem);

                    // 删除购物车中的商品
                    cartService.deleteCartItem(omsCartItem);
                }
            }
            omsOrder.setOmsOrderItems(omsOrderItemList);

            // 将订单和订单项写入数据库
            orderService.saveOrder(omsOrder);


            // TODO 进入支付系统
        } else {
            return "tradeFail";
        }

        return null;
    }

    @RequestMapping("toTrade")
    @LoginRequired(loginSuccess = true)
    public String toTrade(HttpServletRequest request, ModelMap modelMap) {

        String memberId = (String) request.getAttribute("memberId");

        List<UmsMemberReceiveAddress> memberReceiveAddressList = userService.getReceiveAddressByMemberId(memberId);
        // 将购物车集合转化为页面计算清单集合
        List<OmsCartItem> omsCartItemList = cartService.cartList(memberId);
        List<OmsOrderItem> omsOrderItemList = new ArrayList<>();

        for (OmsCartItem omsCartItem : omsCartItemList) {
            if ("1".equals(omsCartItem.getIsChecked())) {
                OmsOrderItem omsOrderItem = new OmsOrderItem();
                omsOrderItem.setProductName(omsCartItem.getProductName());
                omsOrderItem.setProductPic(omsCartItem.getProductPic());
                omsOrderItemList.add(omsOrderItem);
            }
        }

        modelMap.put("omsOrderItems", omsOrderItemList);
        modelMap.put("userAddressList", memberReceiveAddressList);
        modelMap.put("totalAmount", getTotalAmount(omsCartItemList));
        // 添加交易码  防止订单重复提交
        String tradeCode = orderService.getTradeCode(memberId);
        modelMap.put("tradeCode", tradeCode);
        return "trade";
    }

    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItems) {
        BigDecimal totalAmount = new BigDecimal("0");

        for (OmsCartItem omsCartItem : omsCartItems) {
            BigDecimal totalPrice = omsCartItem.getTotalPrice();

            if(omsCartItem.getIsChecked().equals("1")){
                totalAmount = totalAmount.add(totalPrice);
            }
        }

        return totalAmount;
    }
}
