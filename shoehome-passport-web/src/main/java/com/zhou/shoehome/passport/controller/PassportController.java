package com.zhou.shoehome.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.zhou.shoehome.bean.UmsMember;
import com.zhou.shoehome.service.IUserService;
import com.zhou.shoehome.util.HttpClientUtil;
import com.zhou.shoehome.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhouzh6
 */
@Controller
@CrossOrigin
public class PassportController {

    @Reference
    IUserService userService;

    @RequestMapping("vlogin")
    public String vlogin(String code, HttpServletRequest request) {
        // 授权码获取access_token
        String s3 = "https://api.weibo.com/oauth2/access_token?";
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("client_id", "");
        paramMap.put("client_secret", "");
        paramMap.put("grant_type", "authorization_code");
        paramMap.put("redirect_uri", "http://47.110.60.206:8085/vlogin");
        // 授权有效期内可以使用，没新生成一次授权码，说明用户对第三方数据进行重启授权，之前的access_token和授码全部过期
        paramMap.put("code", code);
        String accessTokenJson = HttpClientUtil.doPost(s3, paramMap);
        Map<String, Object> accessMap = JSON.parseObject(accessTokenJson, Map.class);

        // access_token换取用户数据
        String uid = (String) accessMap.get("uid");
        String accessToken = (String) accessMap.get("access_token");
        String showUserUrl = "https://api.weibo.com/2/users/show.json?access_token=" + accessToken + "&uid=" + uid;
        String userJson = HttpClientUtil.doGet(showUserUrl);
        Map<String, Object> userMap = JSON.parseObject(userJson, Map.class);

        // 将用户数据保存到数据库
        UmsMember umsMember = new UmsMember();
        umsMember.setSourceType("2");
        umsMember.setAccessCode(code);
        umsMember.setAccessToken(accessToken);
        umsMember.setSourceUid((String) userMap.get("idstr"));
        umsMember.setCity((String) userMap.get("location"));
        umsMember.setNickname((String) userMap.get("screen_name"));
        String g = "0";
        String gender = (String) userMap.get("gender");
        if (gender.equals("m")) {
            g = "1";
        }
        umsMember.setGender(g);

        UmsMember umsCheck = new UmsMember();
        umsCheck.setSourceUid(umsMember.getSourceUid());
        // 检查该用户(社交用户)以前是否登陆过系统
        UmsMember umsMemberCheck = userService.checkOauthUser(umsCheck);

        if (umsMemberCheck == null) {
            umsMember = userService.addOauthUser(umsMember);
        } else {
            umsMember = umsMemberCheck;
        }

        // 按照设计的算法对参数进行加密后，生成token
        String token = getTokenByUmsMember(umsMember, request);
        // 将token存入redis一份
        userService.addUserToken(umsMember.getId(), token);

        return "redirect:http://47.110.60.206:8083/index";
    }

    private String getTokenByUmsMember(UmsMember umsMember, HttpServletRequest request) {
        // 生成jwt的token 重定向到首页 携带该token
        // rpc的主键返回策略失效
        String memberId = umsMember.getId();
        String nickname = umsMember.getNickname();
        Map<String, Object> userMapToken = new HashMap<>();
        // 是保存数据库后主键返回策略生成的id
        userMapToken.put("memberId", memberId);
        userMapToken.put("nickname", nickname);

        // 通过nginx转发的客户端ip
        String ip = request.getHeader("x-forwarded-for");
        if (StringUtils.isBlank(ip)) {
            ip = request.getRemoteAddr();// 从request中获取ip
            if (StringUtils.isBlank(ip)) {
                ip = "127.0.0.1";
            }
        }
        return JwtUtil.encode("shoehome", userMapToken, ip);
    }

    @RequestMapping("verify")
    @ResponseBody
    public String verify(String token, String salt) {
        Map<String, String> map = new HashMap<>();
        Map<String, Object> deCode = JwtUtil.decode(token, "shoehome", salt);

        if (deCode != null) {
            map.put("status", "success");
            map.put("memberId", (String) deCode.get("memberId"));
            map.put("nickname", (String) deCode.get("nickname"));
        } else {
            map.put("status", "fail");
        }

        return JSON.toJSONString(map);
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UmsMember umsMember, HttpServletRequest request) {
        UmsMember umsMemberLogin = userService.login(umsMember);
        if (umsMemberLogin != null) {
            String token = getTokenByUmsMember(umsMember, request);
            // 将token存入缓存中
            userService.addUserToken(umsMember.getId(), token);
            return token;
        } else {
            // 登录失败
            return "fail";
        }
    }

    @RequestMapping("index")
    @ResponseBody
    public String index(String ReturnUrl, ModelMap modelMap) {
        if (StringUtils.isNotBlank(ReturnUrl)) {
            modelMap.put("ReturnUrl", ReturnUrl);
        }
        return "index";
    }
}