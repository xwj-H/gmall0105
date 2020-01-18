package cn.tedu.gmall.service;


import cn.tedu.gmall.bean.UmsMember;
import cn.tedu.gmall.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UserService {


    List<UmsMember> getAllUser();

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);
}
