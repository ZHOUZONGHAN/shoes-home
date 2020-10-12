package com.zhou.shoehome.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.zhou.shoehome.bean.PmsBaseAttrInfo;
import com.zhou.shoehome.bean.PmsBaseAttrValue;
import com.zhou.shoehome.bean.PmsBaseSaleAttr;
import com.zhou.shoehome.service.IAttrService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @author zhouzh6
 */
@Controller
@CrossOrigin
public class AttrController {

    @Reference
    IAttrService attrService;

    @RequestMapping("baseSaleAttrList")
    @ResponseBody
    public List<PmsBaseSaleAttr> baseSaleAttrList() {
        return attrService.baseSaleAttrList();
    }

    @RequestMapping("getAttrValueList")
    @ResponseBody
    public List<PmsBaseAttrValue> getAttrValueList(String attrId) {
        return attrService.getAttrValueList(attrId);
    }

    @RequestMapping("saveAttrInfo")
    @ResponseBody
    public String saveAttrInfo(@RequestBody PmsBaseAttrInfo pmsBaseAttrInfo) {
        return attrService.saveAttrInfo(pmsBaseAttrInfo);
    }

    @RequestMapping("attrInfoList")
    @ResponseBody
    public List<PmsBaseAttrInfo> attrInfoList(String catalog3Id) {
        return attrService.attrInfoList(catalog3Id);
    }
}
