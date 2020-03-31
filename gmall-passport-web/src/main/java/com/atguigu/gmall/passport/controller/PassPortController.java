package com.atguigu.gmall.passport.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpClientUtil;
import com.atguigu.gmall.util.JwtUtil;
import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;


/**
 * @author yangkun
 * @date 2020/3/13
 */
@Controller
public class PassPortController {
    @Reference
    UserService userService;

    /**
     * 接入微博接口
     * @param code
     * @param request
     * @return
     */
    @RequestMapping("vlogin")
    public String vlogin(String code, HttpServletRequest request, HttpServletResponse response){
        //接受index.html返回的code
        String s2 = "https://api.weibo.com/oauth2/access_token";
        Map<String,String> map = new HashMap<>();
        map.put("client_id","3254588250");
        map.put("client_secret","9dbcc2becabad959b6cf6b5779e1c35c");
        map.put("grant_type","authorization_code");
        map.put("code",code);
        map.put("redirect_uri","http://passport.gmall.com:8088/vlogin");
        //再去发送post请求获取access_token
        String access_token_info = HttpClientUtil.doPost(s2,map);
        Map<String,Object> access_map = JSON.parseObject(access_token_info,Map.class);
        //获取access_token里面的相关信息
        String uid =(String)access_map.get("uid");
        String access_token =(String)access_map.get("access_token");
        String user_info_url ="https://api.weibo.com/2/users/show.json?access_token="+access_token+"&uid="+uid;
        //发送get请求获取用户信息
        String user_information = HttpClientUtil.doGet(user_info_url);
        Map<String,Object> user_map = JSON.parseObject(user_information,Map.class);
        //生成用户信息
        UmsMember umsMember = new UmsMember();
        umsMember.setSourceType("2");
        umsMember.setAccessCode(code);
        umsMember.setAccessToken(access_token);
        umsMember.setSourceUid((String)user_map.get("idstr"));
        umsMember.setCity((String)user_map.get("location"));
        umsMember.setNickname((String)user_map.get("screen_name"));
        String g = "0";
        String gender = (String)user_map.get("gender");
        if(gender.equals("m")){
            g = "1";
        }
        umsMember.setGender(g);
        UmsMember umsMemberCheck = new UmsMember();

        //检查用户信息是否登陆,根据微博传过来的uid进行判断,每个用户的uid是唯一的
        umsMemberCheck.setSourceUid(umsMember.getSourceUid());
        UmsMember userCheck = userService.checkOauthUser(umsMemberCheck);
        System.out.println(userCheck);
        if(userCheck == null){
          umsMember = userService.addOauthUser(umsMember);
        }else{
          umsMember = umsMemberCheck;
        }
        //jwt生成token
        Map<String,Object> jwt_map = new HashMap<>();
        jwt_map.put("memberId",umsMember.getId());
        jwt_map.put("nickname",umsMember.getNickname());
        String ip = request.getHeader("x-forwarded-for");
        if(StringUtils.isBlank(ip)){
            ip = request.getRemoteAddr();
            if(StringUtils.isBlank(ip)){
                ip ="127.0.0.1";
            }
        }
        String token =JwtUtil.encode("2020gmall0316",jwt_map,ip);
        //将用户信息添加到缓存
        CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
        userService.addTokenCache(token,umsMember.getId());
        //登陆完成后重定向页面
        return "redirect:http://search.gmall.com:8083/index?token="+token;
    }
    @RequestMapping("verify")
    @ResponseBody
    public String verify(@RequestParam("token") String token, String currentIp){
        //通过jwt验证真假
        Map<String,String> map=new HashMap<>();
        Map<String, Object> decode = JwtUtil.decode(token, "2020gmall0316", currentIp);
        if(decode !=null){
            map.put("status","success");
            map.put("memberId",(String)decode.get("memberId"));
            map.put("nickname",(String)decode.get("nickname"));
        }else{
            map.put("status","fail");
        }
        return JSON.toJSONString(map);
    }
    @RequestMapping("login")
    @ResponseBody
    public String login(UmsMember umsMember, HttpServletRequest request,HttpServletResponse response){
      //验证用户信息
        String token="";
       UmsMember user = userService.login(umsMember);
       if(user!=null){

          //说明验证成功制作token
          Map<String,Object> map =new HashMap<>();
          map.put("memberId",user.getId());
          map.put("nickname",user.getNickname());


          String ip=request.getHeader("x-forwarded-for");//通过nginx转发的客户端ip
          if(StringUtils.isBlank(ip)){
              ip=request.getRemoteAddr();//得到request请求的url;
              if(StringUtils.isBlank(ip)){
                 ip="127.0.0.1";
              }
          }


          token = JwtUtil.encode("2020gmall0316",map,ip);
          //将生成的token放在cookie中
           CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
           userService.addTokenCache(token,user.getId());
           return token;
      }else{
          return "fail";
      }
    }
    @RequestMapping("index")
    public String index(String ReturnUrl, ModelMap map){
        map.put("ReturnUrl",ReturnUrl);
        return "index";
    }
}
