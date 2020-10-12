package com.zhou.shoehome.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.zhou.shoehome.bean.OmsCartItem;
import com.zhou.shoehome.cart.mapper.OmsCartItemMapper;
import com.zhou.shoehome.service.ICartService;
import com.zhou.shoehome.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhouzh6
 */
@Service
public class CartServiceImpl implements ICartService {

    @Autowired
    OmsCartItemMapper omsCartItemMapper;

    @Autowired
    RedisUtil redisUtil;

    @Transactional
    @Override
    public void deleteCartItem(OmsCartItem omsCartItem) {
        omsCartItemMapper.delete(omsCartItem);
    }

    @Transactional
    @Override
    public void checkCart(OmsCartItem omsCartItem) {
        Example example = new Example(OmsCartItem.class);
        example.createCriteria()
                .andEqualTo("memberId", omsCartItem.getMemberId())
                .andEqualTo("productSkuId", omsCartItem.getProductSkuId());

        omsCartItemMapper.updateByExampleSelective(omsCartItem, example);

        // 缓存同步
        synCache(omsCartItem.getMemberId());
    }

    @Override
    public List<OmsCartItem> cartList(String userId) {

        // 使用try with resources格式，因为Jedis实现了Closeable接口
        try (Jedis jedis = redisUtil.getJedis()) {
            List<OmsCartItem> omsCartItemList = new ArrayList<>();
            List<String> hvals = jedis.hvals("user:" + userId + ":cart");
            for (String hval : hvals) {
                OmsCartItem omsCartItem = JSON.parseObject(hval, OmsCartItem.class);
                omsCartItemList.add(omsCartItem);
            }
            return omsCartItemList;
        }
    }

    @Override
    public OmsCartItem getCartsByUser(String memberId, String skuId) {
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        omsCartItem.setProductSkuId(skuId);
        return omsCartItemMapper.selectOne(omsCartItem);
    }

    @Override
    public void addCart(OmsCartItem omsCartItemFromDb) {
        if (StringUtils.isNotBlank(omsCartItemFromDb.getMemberId())) {
            omsCartItemMapper.insert(omsCartItemFromDb);
        }
    }

    @Override
    public void updateCart(OmsCartItem omsCartItemFromDb) {
        Example example = new Example(OmsCartItem.class);
        example.createCriteria().andEqualTo("id", omsCartItemFromDb.getId());
        omsCartItemMapper.updateByExampleSelective(omsCartItemFromDb, example);
    }

    @Transactional
    @Override
    public void synCache(String memberId) {
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);
        List<OmsCartItem> omsCartItemList = omsCartItemMapper.select(omsCartItem);

        // 使用try with resources格式，因为Jedis实现了Closeable接口
        try (Jedis jedis = redisUtil.getJedis()) {
            // 同步到Redis中
            String cacheKey = "user:" + memberId + ":cart";
            Map<String, String> cacheValue = new HashMap<>();
            for (OmsCartItem cartItem : omsCartItemList) {
                cacheValue.put(cartItem.getProductSkuId(), JSON.toJSONString(cartItem));
            }
            jedis.del(cacheKey);
            jedis.hmset(cacheKey, cacheValue);
        }
    }
}
