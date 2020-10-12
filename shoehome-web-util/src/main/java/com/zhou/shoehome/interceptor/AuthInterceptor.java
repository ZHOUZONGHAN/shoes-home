package com.zhou.shoehome.interceptor;

import com.alibaba.fastjson.JSON;
import com.zhou.shoehome.annotations.LoginRequired;
import com.zhou.shoehome.util.CookieUtil;
import com.zhou.shoehome.util.HttpClientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhouzh6
 */
@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    private static final  String OLD_COOKIE_NAME = "oldToken";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 判断方法注解上是否含有@Required注解
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        LoginRequired loginRequired = handlerMethod.getMethodAnnotation(LoginRequired.class);
        if (loginRequired == null) {
            return true;
        }

        String token = "";

        String oldToken = CookieUtil.getCookieValue(request, OLD_COOKIE_NAME, true);
        if (StringUtils.isNotBlank(oldToken)) {
            token = oldToken;
        }

        String newToken = request.getParameter("token");
        if (StringUtils.isNotBlank(newToken)) {
            token = newToken;
        }

        // 是否需要登陆成功才能访问  针对购物车不登录也可以添加商品的设计
        boolean loginSuccess = loginRequired.loginSuccess();

        // 调用认证中心进行验证
        String verifyResult = "fail";
        Map<String, String> verifyResultMap = new HashMap<>();
        if (StringUtils.isNotBlank(token)) {

            String salt = request.getHeader("x-forwarded-for");// 通过nginx转发的客户端ip
            if(StringUtils.isBlank(salt)){
                salt = request.getRemoteAddr();// 从request中获取ip
                if(StringUtils.isBlank(salt)){
                    salt = "127.0.0.1";
                }
            }

            String verifyResultJSON = HttpClientUtil.doGet("47.110.60.206:8085/verify?token=" + token + "&salt=" + salt);

            verifyResultMap = JSON.parseObject(verifyResultJSON, Map.class);

            verifyResult = verifyResultMap.get("status");
        }

        if (loginSuccess) {
            if ("fail".equals(verifyResult)) {
                // token为空  进入用户中心进行认证
                StringBuffer requestURL = request.getRequestURL();
                response.sendRedirect("47.110.60.206:8085/index?ReturnUrl=" + requestURL);
                return false;
            }
            request.setAttribute("memberId", verifyResultMap.get("memberId"));
            request.setAttribute("nickname", verifyResultMap.get("nickname"));
            // 覆盖cookie中的token刷新token有效时间
            if (StringUtils.isNotBlank(token)) {
                CookieUtil.setCookie(request, response, OLD_COOKIE_NAME, newToken, 60 * 30, true);
            }
        } else {
            if ("success".equals(verifyResult)) {
                // 存入临时身份信息
                request.setAttribute("memberId", verifyResultMap.get("memberId"));
                request.setAttribute("nickname", verifyResultMap.get("nickname"));
                // 添加cookie
                if (StringUtils.isNotBlank(token)) {
                    CookieUtil.setCookie(request, response, OLD_COOKIE_NAME, newToken, 60 * 30, true);
                }
            }
        }
        return true;
    }
}
