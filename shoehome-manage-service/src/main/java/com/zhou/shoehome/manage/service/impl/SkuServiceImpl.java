package com.zhou.shoehome.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.zhou.shoehome.bean.PmsSkuAttrValue;
import com.zhou.shoehome.bean.PmsSkuImage;
import com.zhou.shoehome.bean.PmsSkuInfo;
import com.zhou.shoehome.bean.PmsSkuSaleAttrValue;
import com.zhou.shoehome.manage.mapper.PmsSkuAttrValueMapper;
import com.zhou.shoehome.manage.mapper.PmsSkuImageMapper;
import com.zhou.shoehome.manage.mapper.PmsSkuInfoMapper;
import com.zhou.shoehome.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.zhou.shoehome.service.ISkuService;
import com.zhou.shoehome.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author zhouzh6
 */
@Service
public class SkuServiceImpl implements ISkuService {

    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;

    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;

    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;

    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;

    @Autowired
    RedisUtil redisUtil;

    @Transactional
    @Override
    public boolean checkPrice(String productSkuId, BigDecimal productPrice) {
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(productSkuId);
        pmsSkuInfo = pmsSkuInfoMapper.selectOne(pmsSkuInfo);

        if (pmsSkuInfo != null) {
            BigDecimal price = pmsSkuInfo.getPrice();
            if (price.compareTo(productPrice) == 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<PmsSkuInfo> getAllSku() {
        List<PmsSkuInfo> pmsSkuInfoList = pmsSkuInfoMapper.selectAll();

        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfoList) {
            String skuId = pmsSkuInfo.getId();
            PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
            List<PmsSkuAttrValue> pmsSkuAttrValueList = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);
            pmsSkuInfo.setSkuAttrValueList(pmsSkuAttrValueList);
        }
        return pmsSkuInfoList;
    }

    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId) {
        return pmsSkuInfoMapper.selectSkuSaleAttrValueListBySpu(productId);
    }

    @Transactional
    public PmsSkuInfo getSkuByIdFromDb(String skuId) {
        PmsSkuInfo pmsSkuInfo = pmsSkuInfoMapper.selectByPrimaryKey(skuId);

        // 图片集合
        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> pmsSkuImageList = pmsSkuImageMapper.select(pmsSkuImage);
        pmsSkuInfo.setSkuImageList(pmsSkuImageList);
        return pmsSkuInfo;
    }

    @Transactional
    @Override
    public PmsSkuInfo getSkuById(String skuId) {

        Jedis jedis = redisUtil.getJedis();;
        // 缓存key
        String skuKey = "sku:" + skuId + ":info";
        // 分布式锁key
        String skuLock = "sku:" + skuId + ":lock";
        try {
            // 查缓存
            String skuJson = jedis.get(skuKey);
            PmsSkuInfo pmsSkuInfo;
            if (StringUtils.isNotBlank(skuJson)) {
                pmsSkuInfo = JSON.parseObject(skuJson, PmsSkuInfo.class);
            } else {
                // 缓存未命中 查数据库
                // 为了避免大量客户端同时查询数据库  设置分布式锁
                String res = jedis.set(skuLock, "1", "nx", "ex", 10);
                if (StringUtils.isNotBlank(res)) {
                    // 设置成功
                    pmsSkuInfo = getSkuByIdFromDb(skuId);
                    if (pmsSkuInfo != null) {
                        // 将数据库中的值存入Redis
                        jedis.set(skuKey, JSON.toJSONString(pmsSkuInfo));
                    } else {
                        // 数据库中不存在该值  防止恶意访问  设置空值在Redis中或者使用布隆过滤器
                        jedis.setex(skuKey, 60, "");
                    }
                } else {
                    // 未获取到锁则进行自旋
                    return getSkuById(skuId);
                }
            }
            return pmsSkuInfo;
        } finally {
            // 释放分布式锁
            jedis.del(skuLock);
            jedis.close();
        }
    }

    @Transactional
    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {
        // 添加pmsSkuInfo
        pmsSkuInfoMapper.insertSelective(pmsSkuInfo);
        String skuId = pmsSkuInfo.getId();

        // 添加平台属性
        List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();
        for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
            pmsSkuAttrValue.setSkuId(skuId);
            pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
        }

        // 添加销售属性
        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();
        for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
            pmsSkuSaleAttrValue.setSkuId(skuId);
            pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
        }

        // 添加图片
        List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : skuImageList) {
            pmsSkuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }
    }
}
