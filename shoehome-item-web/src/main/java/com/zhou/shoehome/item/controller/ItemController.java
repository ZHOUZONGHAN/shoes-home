package com.zhou.shoehome.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.zhou.shoehome.bean.PmsProductSaleAttr;
import com.zhou.shoehome.bean.PmsSkuAttrValue;
import com.zhou.shoehome.bean.PmsSkuInfo;
import com.zhou.shoehome.bean.PmsSkuSaleAttrValue;
import com.zhou.shoehome.service.ISkuService;
import com.zhou.shoehome.service.ISpuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhouzh6
 */
@Controller
@CrossOrigin
public class ItemController {

    @Reference
    ISkuService skuService;

    @Reference
    ISpuService spuService;

    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, ModelMap modelMap) {

        PmsSkuInfo pmsSkuInfo = skuService.getSkuById(skuId);

        // sku对象
        modelMap.put("skuInfo", pmsSkuInfo);

        // 销售属性
        List<PmsProductSaleAttr> pmsProductSaleAttrList = spuService.getSpuSaleAttrListCheckBySku(pmsSkuInfo.getProductId(), skuId);
        modelMap.put("spuSaleAttrListCheckBySku", pmsProductSaleAttrList);

        // 查询同一spu的sku集合
        List<PmsSkuInfo> pmsSkuInfoList = skuService.getSkuSaleAttrValueListBySpu(pmsSkuInfo.getProductId());

        Map<String, String> skuSaleAttrMap = new HashMap<>();
        for (PmsSkuInfo skuInfo : pmsSkuInfoList) {
            String value = skuInfo.getId();

            List<PmsSkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();

            String key = "";
            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
                key += pmsSkuSaleAttrValue.getSaleAttrValueId() + "|";
            }

            skuSaleAttrMap.put(key, value);
        }

        String skuSaleAttrMapJson = JSON.toJSONString(skuSaleAttrMap);

        modelMap.put("skuSaleAttrMapJson", skuSaleAttrMapJson);

        return "item";
    }
}
