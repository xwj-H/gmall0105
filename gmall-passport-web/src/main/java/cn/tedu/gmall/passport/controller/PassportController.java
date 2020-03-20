package cn.tedu.gmall.passport.controller;

import cn.tedu.gmall.bean.UmsMember;
import cn.tedu.gmall.service.UserService;
import cn.tedu.gmall.util.HttpclientUtil;
import cn.tedu.gmall.util.JwtUtil;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    UserService userService;

    @RequestMapping("vlogin")
    @ResponseBody
    public String vlogin(String code, HttpServletRequest request) {

        //授权码换取access_token
        // 换取access_token
        // client_secret=a79777bba04ac70d973ee002d27ed58c
        // client_id=187638711
        String s3 = "https://api.weibo.com/oauth2/access_token?";//?client_id=187638711&client_secret
        // =a79777bba04ac70d973ee002d27ed58c&grant_type=authorization_code&redirect_uri=http://passport.gmall
        // .com:8085/vlogin&code=CODE";
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("client_id", "187638711");
        paramMap.put("client_secret", "a79777bba04ac70d973ee002d27ed58c");
        paramMap.put("grant_type", "authorization_code");
        paramMap.put("redirect_uri", "http://passport.gmall.com:8085/vlogin");
        paramMap.put("code", code);// 授权有效期内可以使用，没新生成一次授权码，说明用户对第三方数据进行重启授权，之前的access_token和授权码全部过期
        String access_token_json = HttpclientUtil.doPost(s3, paramMap);

        Map<String, Object> access_map = JSON.parseObject(access_token_json, Map.class);

        //access_token换取用户信息
        String uid = (String) access_map.get("uid");
        String access_token = (String) access_map.get("access_token");
        String show_user_url = "https://api.weibo.com/2/users/show.json?access_token=" + access_token + "&uid=" + uid;
        String user_json = HttpclientUtil.doGet(show_user_url);

        Map<String, Object> user_map = JSON.parseObject(user_json, Map.class);


        //将用户信息保存数据库，用户类型设置为微博用户
        UmsMember umsMember = new UmsMember();
        umsMember.setSourceType(2);
        umsMember.setAccessCode(code);
        umsMember.setAccessToken(access_token);
        umsMember.setSourceUid((Long) user_map.get("id"));
        umsMember.setCity((String) user_map.get("location"));
        umsMember.setNickname((String) user_map.get("screen_name"));

        int g = 0;
        String gender = (String) user_map.get("gender");
        if (gender.equals("m")) {
            g = 1;
        }
        umsMember.setGender(g);

        UmsMember umsCheck = new UmsMember();
        umsCheck.setSourceUid(umsMember.getSourceUid());

        UmsMember umsMemberCheck = userService.checkaOuthUser(umsCheck);
        if (umsMemberCheck == null) {
            umsMember = userService.addOauthUser(umsMember);
        } else {
            umsMember = umsMemberCheck;
        }

        //生成jwt的token，并且重定向到首页，携带该token
        String memberId = umsMember.getId(); //rpc主键的返回策略失效
        String nickname = umsMember.getNickname();

        String token = getUserToken(request, memberId, nickname);

        return "redirect:http://localhost:8083/index.html?token" + token;
    }

    @RequestMapping("verify")
    @ResponseBody
    public String verify(String token, String currentIp) {

        //通过jwt校验token真假
        Map<String, String> map = new HashMap<>();


        Map<String, Object> decode = JwtUtil.decode(token, "2019gmall0105", currentIp);

        if (decode != null) {
            map.put("status", "success");
            map.put("memberId", String.valueOf(decode.get("memberId")));
            map.put("memberId", String.valueOf(decode.get("nickname")));
        } else {
            map.put("status", "fail");

        }

        return JSON.toJSONString(map);
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UmsMember umsMember, HttpServletRequest request) {

        String token = "";

        //调用用户服务验证用户名和密码
        UmsMember umsMemberLogin = userService.login(umsMember);
        if (umsMemberLogin != null) {
            //登录成功

            //用jwt制作token
            String memberId = umsMemberLogin.getId();
            String nickname = umsMemberLogin.getNickname();

            getUserToken(request, memberId, nickname);

        } else {
            //登录失败
        }
        return "token";
    }

    private String getUserToken(HttpServletRequest request, String memberId, String nickname) {
        String token = null;
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("memberId", memberId); // 是保存数据库后主键返回策略生成的id
        userMap.put("nickname", nickname);
        String ip = request.getHeader("x-forwarded-for");//通过nginx转发的客户端ip
        if (StringUtils.isBlank(ip)) {
            ip = request.getRemoteAddr();//从request中获取ip
            if (StringUtils.isBlank(ip)) {
                ip = "127.0.0.1";//这里都没有获取ip要处理异常
            }
        }

        //按照设计的算法对参数进行加密后，生成token
        token = JwtUtil.encode("2019gmall0105", userMap, ip);

        //将token存入redis一份
        userService.addUserToken(token, memberId);
        return token;
    }

    @RequestMapping("index")
    public String index(String ReturnUrl, ModelMap modelMap) {
        modelMap.addAttribute("ReturnUrl", ReturnUrl);
        return "index";
    }


}


