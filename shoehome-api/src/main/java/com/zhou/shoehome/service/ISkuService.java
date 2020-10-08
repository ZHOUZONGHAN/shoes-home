package com.zhou.shoehome.service;

import com.zhou.shoehome.bean.PmsSkuInfo;

import java.util.List;

/**
 * @author zhouzh6
 * @date 2020-10-05
 */
public interface ISkuService {

    void saveSkuInfo(PmsSkuInfo pmsSkuInfo);

    PmsSkuInfo getSkuById(String skuId);

    List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId);

    List<PmsSkuInfo> getAllSku();
}
