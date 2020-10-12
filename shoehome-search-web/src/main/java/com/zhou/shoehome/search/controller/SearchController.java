package com.zhou.shoehome.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.zhou.shoehome.annotations.LoginRequired;
import com.zhou.shoehome.bean.*;
import com.zhou.shoehome.service.IAttrService;
import com.zhou.shoehome.service.ISearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zhouzh6
 */
@Controller
@CrossOrigin
public class SearchController {

    @Reference
    ISearchService searchService;

    @Reference
    IAttrService attrService;

    @RequestMapping("list.html")
    public String list(PmsSearchParam pmsSearchParam, ModelMap modelMap) {
        List<PmsSearchSkuInfo> pmsSearchSkuInfoList = searchService.list(pmsSearchParam);
        modelMap.put("skuLsInfoList", pmsSearchSkuInfoList);

        // 获取所有结果中的平台属性集合
        List<String> valueIdList = pmsSearchSkuInfoList
                .stream()
                .flatMap(pmsSearchSkuInfo ->
                        pmsSearchSkuInfo
                                .getSkuAttrValueList()
                                .stream()
                                .map(PmsSkuAttrValue::getAttrId))
                .distinct()
                .collect(Collectors.toList());

        // 根据valueIdList将属性列表查出来
        List<PmsBaseAttrInfo> pmsBaseAttrInfoList = attrService.getAttrValueListByValueId(valueIdList);
        modelMap.put("attrList", pmsBaseAttrInfoList);

        // 去掉当前条件中valueId所在的属性组
        String[] delValueIds = pmsSearchParam.getValueId();
        if (delValueIds != null) {
            List<PmsSearchCrumb> pmsSearchCrumbList = new ArrayList<>();
            for (String delValueId : delValueIds) {
                Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfoList.iterator();
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                // 生成面包屑参数
                pmsSearchCrumb.setValueId(delValueId);
                pmsSearchCrumb.setUrlParam(getUrlParamForCrumb(pmsSearchParam, delValueId));
                while (iterator.hasNext()) {
                    PmsBaseAttrInfo pmsBaseAttrInfo = iterator.next();
                    List<PmsBaseAttrValue> attrValueList = pmsBaseAttrInfo.getAttrValueList();
                    for (PmsBaseAttrValue pmsBaseAttrValue : attrValueList) {
                        String pmsBaseAttrValueId = pmsBaseAttrValue.getId();
                        if (delValueId.equals(pmsBaseAttrValueId)) {
                            // 查找面包屑的属性值名称
                            pmsSearchCrumb.setValueName(pmsBaseAttrValue.getValueName());
                            iterator.remove();
                        }
                    }
                }
                pmsSearchCrumbList.add(pmsSearchCrumb);
            }
            modelMap.put("attrValueSelectedList", pmsSearchCrumbList);
        }

        String urlParam = getUrlParam(pmsSearchParam);
        modelMap.put("urlParam", urlParam);

        String keyword = pmsSearchParam.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            modelMap.put("keyword", keyword);
        }

        return "list";
    }

    private String getUrlParamForCrumb(PmsSearchParam pmsSearchParam, String delCrumbValue) {
        StringBuilder urlParam = new StringBuilder();

        String keyword = pmsSearchParam.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            if (StringUtils.isNotBlank(urlParam.toString())) {
                urlParam.append("&");
            }
            urlParam.append("keyword=").append(keyword);
        }

        String catalog3Id = pmsSearchParam.getCatalog3Id();
        if (StringUtils.isNotBlank(catalog3Id)) {
            if (StringUtils.isNotBlank(urlParam.toString())) {
                urlParam.append("&");
            }
            urlParam.append("&").append(catalog3Id);
        }

        String[] valueIds = pmsSearchParam.getValueId();
        if (valueIds != null) {
            for (String valueId : valueIds) {
                if (delCrumbValue != null && !valueId.equals(delCrumbValue)) {
                    urlParam.append("&valueId=").append(valueId);
                }
            }
        }

        return urlParam.toString();
    }

    private String getUrlParam(PmsSearchParam pmsSearchParam) {
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String[] skuAttrValueList = pmsSearchParam.getValueId();

        StringBuilder urlParam = new StringBuilder();

        if (StringUtils.isNotBlank(keyword)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam.append("&");
            }
            urlParam.append("keyword=").append(keyword);
        }

        if (StringUtils.isNotBlank(catalog3Id)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam.append("&");
            }
            urlParam.append("catalog3Id=").append(catalog3Id);
        }

        if (skuAttrValueList != null) {

            for (String pmsSkuAttrValue : skuAttrValueList) {
                urlParam.append("&valueId=").append(pmsSkuAttrValue);
            }
        }

        return urlParam.toString();
    }

    @RequestMapping("index")
    @LoginRequired(loginSuccess = false)
    public String index() {
        return "index";
    }
}
