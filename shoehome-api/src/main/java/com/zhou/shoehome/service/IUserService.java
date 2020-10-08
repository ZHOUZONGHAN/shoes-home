package com.zhou.shoehome.service;

import com.zhou.shoehome.bean.UmsMember;
import com.zhou.shoehome.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface IUserService {

    List<UmsMember> getAllUser();

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);
}
