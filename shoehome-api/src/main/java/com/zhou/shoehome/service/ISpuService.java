package com.zhou.shoehome.service;

import com.zhou.shoehome.bean.PmsProductImage;
import com.zhou.shoehome.bean.PmsProductInfo;
import com.zhou.shoehome.bean.PmsProductSaleAttr;

import java.util.List;

/**
 * @author zhouzh6
 * @date 2020-10-04
 */
public interface ISpuService {

    List<PmsProductInfo> spuList(String catalog3Id);

    void saveSpuInfo(PmsProductInfo pmsProductInfo);

    List<PmsProductSaleAttr> spuSaleAttrList(String spuId);

    List<PmsProductImage> spuImageList(String spuId);

    List<PmsProductSaleAttr> getSpuSaleAttrListCheckBySku(String productId, String skuId);
}
