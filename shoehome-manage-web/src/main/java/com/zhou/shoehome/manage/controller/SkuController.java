package com.zhou.shoehome.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.zhou.shoehome.bean.PmsSkuInfo;
import com.zhou.shoehome.service.ISkuService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author zhouzh6
 */
@Controller
@CrossOrigin
public class SkuController {

    @Reference
    ISkuService skuService;

    @RequestMapping("saveSkuInfo")
    @ResponseBody
    public String saveSkuInfo(@RequestBody PmsSkuInfo pmsSkuInfo) {
        // 兼容前端  将spuId赋值给productId
        pmsSkuInfo.setProductId(pmsSkuInfo.getSpuId());

        // 设置默认图片
        if (StringUtils.isBlank(pmsSkuInfo.getSkuDefaultImg())) {
            pmsSkuInfo.setSkuDefaultImg(pmsSkuInfo.getSkuImageList().get(0).getImgUrl());
        }
        skuService.saveSkuInfo(pmsSkuInfo);
        return "success";
    }
}
