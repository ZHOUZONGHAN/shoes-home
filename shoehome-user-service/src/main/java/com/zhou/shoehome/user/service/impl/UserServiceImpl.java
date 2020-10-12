package com.zhou.shoehome.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.zhou.shoehome.bean.UmsMember;
import com.zhou.shoehome.bean.UmsMemberReceiveAddress;
import com.zhou.shoehome.service.IUserService;
import com.zhou.shoehome.user.mapper.UmsMemberReceiveAddressMapper;
import com.zhou.shoehome.user.mapper.UserMapper;
import com.zhou.shoehome.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserServiceImpl implements IUserService {

    @Autowired
    UserMapper userMapper;

    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;

    @Autowired
    RedisUtil redisUtil;

    @Transactional
    @Override
    public UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId) {
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setId(receiveAddressId);
        return umsMemberReceiveAddressMapper.selectOne(umsMemberReceiveAddress);
    }

    @Override
    public UmsMember checkOauthUser(UmsMember umsCheck) {
        return userMapper.selectOne(umsCheck);
    }

    @Override
    public UmsMember addOauthUser(UmsMember umsMember) {
        userMapper.insertSelective(umsMember);
        // Mybatis中主键返回策略不能跨PRC返回  所以需要返回umsMember
        return umsMember;
    }

    @Override
    public void addUserToken(String memberId, String token) {
        try (Jedis jedis = redisUtil.getJedis()) {
            jedis.setex("user:" + memberId + ":token", 60 * 30, token);
        }
    }

    @Override
    public UmsMember login(UmsMember umsMember) {

        Jedis jedis = redisUtil.getJedis();
        String userCacheKey = "user:" + umsMember.getUsername() + umsMember.getPassword() + ":passport";
        if (jedis != null) {
            try {
                String userMemberStr = jedis.get(userCacheKey);
                if (StringUtils.isNotBlank(userMemberStr)) {
                    return JSON.parseObject(userMemberStr, UmsMember.class);
                }

                // 缓存中不存在
                UmsMember userMember = loginFromDb(umsMember);
                if (userMember != null) {
                    jedis.setex(userCacheKey, 60 * 30,JSON.toJSONString(userMember));
                }
                return userMember;
            } finally {
                jedis.close();
            }
        }

        // Redis服务宕机（使用本地缓存/直接查询数据库/服务降级限流）
        return loginFromDb(umsMember);
    }

    private UmsMember loginFromDb(UmsMember umsMember) {
        return userMapper.selectOne(umsMember);
    }

    @Override
    public List<UmsMember> getAllUser() {
        return userMapper.selectAll();
    }

    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {

        // 封装的参数对象
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);

        return umsMemberReceiveAddresses;
    }
}
