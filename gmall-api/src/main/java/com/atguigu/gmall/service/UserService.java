package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {

    List<UmsMember> getAllUser();

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);

    UmsMember login(UmsMember umsMember);

    void addTokenCache(String token,String memberId);

    UmsMember checkOauthUser(UmsMember umsMemberCheck);

    UmsMember addOauthUser(UmsMember umsMember);

    UmsMemberReceiveAddress getReceiceAddress(String receiveAddressId);
}
