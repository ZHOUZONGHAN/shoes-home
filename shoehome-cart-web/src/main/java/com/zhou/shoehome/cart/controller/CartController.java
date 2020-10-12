package com.zhou.shoehome.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.zhou.shoehome.annotations.LoginRequired;
import com.zhou.shoehome.bean.OmsCartItem;
import com.zhou.shoehome.bean.PmsSkuInfo;
import com.zhou.shoehome.service.ICartService;
import com.zhou.shoehome.service.ISkuService;
import com.zhou.shoehome.util.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author zhouzh6
 */
@Controller
@CrossOrigin
public class CartController {

    @Reference
    ISkuService skuService;

    @Reference
    ICartService cartService;

    private static final String COOKIE_NAME = "cartListCookie";

    @RequestMapping("checkCart")
    @LoginRequired(loginSuccess = false)
    public String checkCart(String isCheck, String skuId, HttpServletRequest request, ModelMap modelMap) {
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setIsChecked(isCheck);
        // 修改选中状态
        cartService.checkCart(omsCartItem);

        // 缓存中查找数据
        List<OmsCartItem> omsCartItemList = cartService.cartList(memberId);
        modelMap.put("cartList", omsCartItemList);

        // 总计

        BigDecimal totalAmount = getTotalAmount(omsCartItemList);
        modelMap.put("totalAmount", totalAmount);

        return "cartListInner";
    }

    private BigDecimal getTotalAmount(List<OmsCartItem> omsCartItemList) {
        BigDecimal totalAomunt = new BigDecimal("0");
        for (OmsCartItem omsCartItem : omsCartItemList) {
            if ("1".equals(omsCartItem.getIsChecked())) {
                totalAomunt.add(omsCartItem.getTotalPrice());
            }
        }
        return totalAomunt;
    }

    @RequestMapping("cartList")
    @LoginRequired(loginSuccess = false)
    public String cartList(HttpServletRequest request, ModelMap modelMap) {

        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        List<OmsCartItem> omsCartItemList = new ArrayList<>();
        if (StringUtils.isNotBlank(memberId)) {
            // 用户已经登陆
            omsCartItemList = cartService.cartList(memberId);
        } else {
            // 用户没有登陆
            String cookieValue = CookieUtil.getCookieValue(request, COOKIE_NAME, true);
            if (StringUtils.isNotBlank(cookieValue)) {
                omsCartItemList = JSON.parseArray(cookieValue, OmsCartItem.class);
            }
        }

        for (OmsCartItem omsCartItem : omsCartItemList) {
            omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));
        }

        modelMap.put("cartList", omsCartItemList);
        return "cartList";
    }

    @RequestMapping("addToCart")
    @LoginRequired(loginSuccess = false)
    public String addToCart(String skuId, Integer quantity, HttpServletRequest request, HttpServletResponse response) {

        // 获取商品信息
        PmsSkuInfo skuInfo = skuService.getSkuById(skuId);

        // 将商品数据封装为购物车数据
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setCreateDate(new Date());
        omsCartItem.setDeleteStatus(0);
        omsCartItem.setModifyDate(new Date());
        omsCartItem.setPrice(skuInfo.getPrice());
        omsCartItem.setProductAttr("");
        omsCartItem.setProductBrand("");
        omsCartItem.setProductCategoryId(skuInfo.getCatalog3Id());
        omsCartItem.setProductId(skuInfo.getProductId());
        omsCartItem.setProductName(skuInfo.getSkuName());
        omsCartItem.setProductPic(skuInfo.getSkuDefaultImg());
        // 条形码
        omsCartItem.setProductSkuCode("1231231313232");
        omsCartItem.setProductSkuId(skuId);
        omsCartItem.setQuantity(new BigDecimal(quantity));

        // 判断用户是否登陆
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        List<OmsCartItem> omsCartItemList = new ArrayList<>();
        if (StringUtils.isBlank(memberId)) {
            // 没有登陆
            // 从cookie中获取之前购物车数据
            String cartListCookie = CookieUtil.getCookieValue(request, COOKIE_NAME, true);

            // 判断cookie是否存在
            if (StringUtils.isBlank(cartListCookie)) {
                omsCartItemList.add(omsCartItem);
            } else {
                omsCartItemList = JSON.parseArray(cartListCookie, OmsCartItem.class);
                // cookie存在添加进购物车
                cookieExistAddCartItem(omsCartItemList, omsCartItem);
            }

            CookieUtil.setCookie(request, response, COOKIE_NAME, JSON.toJSONString(omsCartItemList), 60 * 60 * 24 * 3, true);
        } else {
            // 已登陆
            // 从db中查出购物车数据
            OmsCartItem omsCartItemFromDb = cartService.getCartsByUser(memberId, skuId);

            if (omsCartItemFromDb == null) {
                omsCartItem.setMemberId(memberId);
                omsCartItem.setMemberNickname("test");
                omsCartItem.setQuantity(new BigDecimal(quantity));
                cartService.addCart(omsCartItem);
            } else {
                omsCartItemFromDb.setQuantity(omsCartItemFromDb.getQuantity().add(omsCartItem.getQuantity()));
                cartService.updateCart(omsCartItemFromDb);
            }
            // 同步缓存
            cartService.synCache(memberId);
        }

        return "redirect:/success.html";
    }

    private void cookieExistAddCartItem(List<OmsCartItem> omsCartItemList, OmsCartItem omsCartItem) {
        // 循环判断该商品之前是否添加过
        for (OmsCartItem cartItem : omsCartItemList) {
            String productId = omsCartItem.getProductId();
            if (productId.equals(cartItem.getProductId())) {
                // 添加过则更新并退出
                cartItem.setQuantity(cartItem.getQuantity().add(omsCartItem.getQuantity()));
                return;
            }
        }
        // 没添加过则添加
        omsCartItemList.add(omsCartItem);
    }
}
