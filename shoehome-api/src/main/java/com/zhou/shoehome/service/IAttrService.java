package com.zhou.shoehome.service;

import com.zhou.shoehome.bean.PmsBaseAttrInfo;
import com.zhou.shoehome.bean.PmsBaseAttrValue;
import com.zhou.shoehome.bean.PmsBaseSaleAttr;

import java.util.List;

/**
 * @author zhouzh6
 * @date 2020-10-04
 */
public interface IAttrService {

    List<PmsBaseAttrInfo> attrInfoList(String catalog3Id);

    String saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo);

    List<PmsBaseAttrValue> getAttrValueList(String attrId);

    List<PmsBaseSaleAttr> baseSaleAttrList();

    List<PmsBaseAttrInfo> getAttrValueListByValueId(List<String> valueIdList);
}
