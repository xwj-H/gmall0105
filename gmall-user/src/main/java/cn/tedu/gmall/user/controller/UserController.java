package cn.tedu.gmall.user.controller;

import cn.tedu.gmall.user.bean.UmsMember;
import cn.tedu.gmall.user.bean.UmsMemberReceiveAddress;
import cn.tedu.gmall.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class UserController {
    @Autowired
    UserService userService;

    @RequestMapping("getReceiveAddressByMemberId")
    @ResponseBody
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId( String memberId) {
        List<UmsMemberReceiveAddress> umsMemberReceiveAddress = userService.getReceiveAddressByMemberId(memberId);
        return umsMemberReceiveAddress;
    }



    @RequestMapping("getAllUser")
    @ResponseBody
    public List<UmsMember> getAllUser() {
       List<UmsMember> umsMembers=userService.getAllUser();
        return umsMembers;
    }


    @RequestMapping("index")
    @ResponseBody
    public String index() {

        return "hello user";
    }

}
