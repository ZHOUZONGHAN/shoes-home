package com.zhou.shoehome.service;

import com.zhou.shoehome.bean.PmsSkuInfo;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author zhouzh6
 */
public interface ISkuService {

    void saveSkuInfo(PmsSkuInfo pmsSkuInfo);

    PmsSkuInfo getSkuById(String skuId);

    List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId);

    List<PmsSkuInfo> getAllSku();

    boolean checkPrice(String productSkuId, BigDecimal productPrice);
}
