package com.zhou.shoehome.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.zhou.shoehome.bean.*;
import com.zhou.shoehome.manage.mapper.PmsBaseAttrInfoMapper;
import com.zhou.shoehome.manage.mapper.PmsBaseAttrValueMapper;
import com.zhou.shoehome.manage.mapper.PmsBaseSaleAttrMapper;
import com.zhou.shoehome.service.IAttrService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhouzh6
 */
@Service
public class AttrServiceImpl implements IAttrService {

    @Autowired
    PmsBaseAttrInfoMapper pmsBaseAttrInfoMapper;

    @Autowired
    PmsBaseAttrValueMapper pmsBaseAttrValueMapper;

    @Autowired
    PmsBaseSaleAttrMapper pmsBaseSaleAttrMapper;

    @Override
    public List<PmsBaseAttrInfo> getAttrValueListByValueId(List<String> valueIdList) {
        String valueIdStr = StringUtils.join(valueIdList, ",");
        return pmsBaseAttrInfoMapper.selectAttrValueListByValueId(valueIdStr);
    }

    @Override
    public List<PmsBaseSaleAttr> baseSaleAttrList() {
        return pmsBaseSaleAttrMapper.selectAll();
    }

    @Override
    public List<PmsBaseAttrValue> getAttrValueList(String attrId) {
        PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
        pmsBaseAttrValue.setAttrId(attrId);
        return pmsBaseAttrValueMapper.select(pmsBaseAttrValue);
    }

    @Transactional
    @Override
    public String saveAttrInfo(PmsBaseAttrInfo pmsBaseAttrInfo) {
        Assert.notEmpty(pmsBaseAttrInfo.getAttrValueList(), "属性值不能为空");
        if (StringUtils.isNotEmpty(pmsBaseAttrInfo.getId())) {
            // 修改属性
            pmsBaseAttrInfoMapper.updateByPrimaryKey(pmsBaseAttrInfo);

            // 删除之前属性值
            PmsBaseAttrValue pmsBaseAttrValueDel = new PmsBaseAttrValue();
            pmsBaseAttrValueDel.setAttrId(pmsBaseAttrInfo.getId());
            pmsBaseAttrValueMapper.delete(pmsBaseAttrValueDel);
        } else {
            pmsBaseAttrInfoMapper.insertSelective(pmsBaseAttrInfo);
        }
        // 重新加入属性值
        for (PmsBaseAttrValue pmsBaseAttrValue : pmsBaseAttrInfo.getAttrValueList()) {
            pmsBaseAttrValue.setAttrId(pmsBaseAttrInfo.getId());
            pmsBaseAttrValueMapper.insert(pmsBaseAttrValue);
        }
        return "success";
    }

    @Override
    public List<PmsBaseAttrInfo> attrInfoList(String catalog3Id) {
        // 获取平台属性
        PmsBaseAttrInfo pmsBaseAttrInfo = new PmsBaseAttrInfo();
        pmsBaseAttrInfo.setCatalog3Id(catalog3Id);
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = pmsBaseAttrInfoMapper.select(pmsBaseAttrInfo);

        // 设置平台属性值
        for (PmsBaseAttrInfo baseAttrInfo : pmsBaseAttrInfos) {
            PmsBaseAttrValue pmsBaseAttrValue = new PmsBaseAttrValue();
            pmsBaseAttrValue.setAttrId(baseAttrInfo.getId());
            List<PmsBaseAttrValue> pmsBaseAttrValues = pmsBaseAttrValueMapper.select(pmsBaseAttrValue);
            baseAttrInfo.setAttrValueList(pmsBaseAttrValues);
        }
        return pmsBaseAttrInfos;
    }
}
