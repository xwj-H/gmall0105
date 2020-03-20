package cn.tedu.gmall.interceptor;

import cn.tedu.gmall.annotations.LoginRequired;
import cn.tedu.gmall.util.CookieUtil;
import cn.tedu.gmall.util.HttpclientUtil;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {


    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //拦截代码

        //判断被拦截的请求的访问的方法的注解（是否是需要拦截的）
        HandlerMethod hm = (HandlerMethod) handler;
        LoginRequired methodAnnotation = hm.getMethodAnnotation(LoginRequired.class);

        //是否拦截
        if (methodAnnotation == null) {

            return true;
        }

        String token = "";

        String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);

        if (StringUtils.isNotBlank(oldToken)) {
            token = oldToken;
        }
        String newToken = request.getParameter("token");
        if (StringUtils.isNotBlank(newToken)) {
            token = newToken;
        }
        //是否必须登录
        boolean loginSuccess = methodAnnotation.loginSuccess();// 获得该请求是否是否必须登录成功

        //调用认证中心进行验证
        String success = "fail";
        Map<String, String> successMap = null;

        if (StringUtils.isNotBlank(token)) {
            String ip = request.getHeader("x-forwarded-for");//通过nginx转发的客户端ip
            if (StringUtils.isBlank(ip)) {
                ip = request.getRemoteAddr();//从request中获取ip
                if (StringUtils.isBlank(ip)) {
                    ip = "127.0.0.1";//这里都没有获取ip要处理异常
                }
            }
            String successJson =
                    HttpclientUtil.doGet("http://localhost:8085/verify?token" + token + "&currentIp=" + ip);
            successMap = JSON.parseObject(successJson, Map.class);
            success = successMap.get("staus");
        }

        if (loginSuccess) {
            //必须登录成功才能使用
            if (!"success".equals(success)) {
                //重定向回passport登录
                StringBuffer requestURL = request.getRequestURL();
                response.sendRedirect("http://localhost:8085/index?ReturnUrI=" + requestURL);
                return false;
            }
            //验证通过，覆盖cookie中的token
            request.setAttribute("memberId", successMap.get("memberId"));
            request.setAttribute("nickname", successMap.get("nickname"));

            //验证通过，覆盖cookie中的token
            if (StringUtils.isNotBlank(token)) {
                CookieUtil.setCookie(request, response, "oldToken", token, 60 * 60 * 2, true);
            }


            return true;

        } else {
            //没有登录也能用，但是必须验证
            if (!"success".equals(success)) {
                //需要将token携带的用户信息写入
                request.setAttribute("memberId", "1");
                request.setAttribute("nickname", "nickname");

                //验证通过，覆盖cookie中的token
                if (StringUtils.isNotBlank(token)) {
                    CookieUtil.setCookie(request, response, "oldToken", token, 60 * 60 * 2, true);
                }


            }
        }


        return true;
    }
}
