package com.atguigu.gmall.passport.controller;

import com.atguigu.gmall.util.HttpClientUtil;
import sun.net.www.http.HttpClient;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yangkun
 * @date 2020/3/18
 */
public class TestAuthor {
    public static void main(String[] args) {
        //client_id：3254588250
        //一、回调地址获得code
        //redirect_uri:http://passport.gmall.com:8088/vlogin
        String s1 = HttpClientUtil.doGet("https://api.weibo.com/oauth2/authorize?client_id=3254588250&response_type=code&redirect_uri=http://passport.gmall.com:8088/vlogin");
        System.out.println(s1);
         //code: b4b4ef6d73272f1e3f3d0a26c12c2dd1
        //client-secret:  9dbcc2becabad959b6cf6b5779e1c35c
        //二、地址二获取access_token,发送post
        //url https://api.weibo.com/oauth2/access_token?client_id=YOUR_CLIENT_ID&client_secret=YOUR_CLIENT_SECRET&grant_type=authorization_code&redirect_uri=YOUR_REGISTERED_REDIRECT_URI&code=CODE
        String s2="https://api.weibo.com/oauth2/access_token";
        Map<String,String> map = new HashMap<>();
        map.put("client_id","3254588250");
        map.put("client_secret","9dbcc2becabad959b6cf6b5779e1c35c");
        map.put("grant_type","authorization_code");
        map.put("code",s1);
        map.put("redirect_uri","http://passport.gmall.com:8088/vlogin");
        String access_token = HttpClientUtil.doPost(s2, map);
        System.out.println(access_token);
        //地址三得到user信息
        String s3 ="https://api.weibo.com/2/users/show.json";
    }
}
