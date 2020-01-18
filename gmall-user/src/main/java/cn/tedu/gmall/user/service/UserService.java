package cn.tedu.gmall.user.service;

import cn.tedu.gmall.user.bean.UmsMember;
import cn.tedu.gmall.user.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {


    List<UmsMember> getAllUser();

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);
}
