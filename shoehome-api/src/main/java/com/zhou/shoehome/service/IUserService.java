package com.zhou.shoehome.service;

import com.zhou.shoehome.bean.UmsMember;
import com.zhou.shoehome.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface IUserService {

    List<UmsMember> getAllUser();

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);

    UmsMember login(UmsMember umsMember);

    void addUserToken(String memberId, String token);

    UmsMember addOauthUser(UmsMember umsMember);

    UmsMember checkOauthUser(UmsMember umsCheck);

    UmsMemberReceiveAddress getReceiveAddressById(String receiveAddressId);
}
