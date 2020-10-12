package com.zhou.shoehome.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.zhou.shoehome.bean.PmsBaseSaleAttr;
import com.zhou.shoehome.bean.PmsProductImage;
import com.zhou.shoehome.bean.PmsProductInfo;
import com.zhou.shoehome.bean.PmsProductSaleAttr;
import com.zhou.shoehome.service.ISpuService;
import com.zhou.shoehome.util.PmsUploadUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author zhouzh6
 */
@Controller
@CrossOrigin
public class SpuController {

    @Reference
    ISpuService spuService;

    @RequestMapping("spuImageList")
    @ResponseBody
    public List<PmsProductImage> spuImageList(String spuId) {
        return spuService.spuImageList(spuId);
    }

    @RequestMapping("spuSaleAttrList")
    @ResponseBody
    public List<PmsProductSaleAttr> spuSaleAttrList(String spuId) {
        return spuService.spuSaleAttrList(spuId);
    }

    @RequestMapping("fileUpload")
    @ResponseBody
    public String fileUpload(@RequestParam("file") MultipartFile multipartFile) {
        return PmsUploadUtil.uploadImage(multipartFile);
    }

    @RequestMapping("saveSpuInfo")
    @ResponseBody
    public String saveSpuInfo(@RequestBody PmsProductInfo pmsProductInfo) {
        spuService.saveSpuInfo(pmsProductInfo);
        return "success";
    }

    @RequestMapping("spuList")
    @ResponseBody
    public List<PmsProductInfo> spuList(String catalog3Id) {
        return spuService.spuList(catalog3Id);
    }
}
