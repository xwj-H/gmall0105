package cn.tedu.gmall.user.service.impl;

import cn.tedu.gmall.service.UserService;
import cn.tedu.gmall.bean.UmsMember;
import cn.tedu.gmall.bean.UmsMemberReceiveAddress;
import cn.tedu.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import cn.tedu.gmall.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserMapper userMapper;
    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;


    @Override
    public List<UmsMember> getAllUser() {
        List<UmsMember> umsMemberList = userMapper.selectAll(); //userMapper.selectAllUser();
        return umsMemberList;
    }

    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {
        /*Example e = new Example(UmsMemberReceiveAddress.class);
        e.createCriteria().andEqualTo("memberId", memberId); // 创建查询规则*/
//        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.selectByExample(umsMemberReceiveAddress);//外键查询

        //封装参数对象
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);

        return umsMemberReceiveAddresses;
    }
}
