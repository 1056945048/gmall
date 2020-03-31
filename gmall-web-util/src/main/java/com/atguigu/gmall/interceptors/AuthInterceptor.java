package com.atguigu.gmall.interceptors;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpClientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;


/**
 * 拦截器---功能比较繁琐
 * @author yangkun
 * @date 2020/3/13
 */
@Component
public class AuthInterceptor implements HandlerInterceptor{

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //拦截代码，判断是否有@loginSuccess注解
        HandlerMethod hm = (HandlerMethod)handler;
        LoginRequired methodAnnotation = hm.getMethodAnnotation(LoginRequired.class);
        if(methodAnnotation == null){
            //说明方法上没有注释
            return true;
        }


        String token = "";
        String oldToken = CookieUtil.getCookieValue(request,"oldToken",true);
        if(StringUtils.isNotBlank(oldToken)){
            token = oldToken;
        }
        String newToken = request.getParameter("token");
        if(StringUtils.isNotBlank(newToken)){
            token = newToken;
        }
        //是否必须登陆
        boolean must = methodAnnotation.loginSuccess();
        //不论是否必须登陆都要验证token
        String success = "fail";
        Map<String,String> successMap = new HashMap<>();
        if(StringUtils.isNotBlank(token)){

            //验证token使用HttpClientUtil工具类（commin-util）
            /**
             * 进行verify时的ip地址应该是客户端请求的ip,所以url后面
             * 加了currentIp这个参量
             */
            String currentIp = request.getHeader("x-forwarded-for");
            if(StringUtils.isBlank(currentIp)){
                currentIp = request.getRemoteAddr();
                if(StringUtils.isBlank(currentIp)){
                    currentIp = "127.0.0.1";
                }
            }

            String successJson = HttpClientUtil.doGet("http://passport.gmall.com:8088/verify?token="+token+"&currentIp="+currentIp);

            if(StringUtils.isNotBlank(successJson)){
                successMap = JSON.parseObject(successJson,Map.class);
            }

            success = successMap.get("status");
        }

        if(must){
            //必须登陆才能访问
            if(!("success".equals(success))){
                //如果验证token失败,重定向到登陆页面
                StringBuffer ReturnUrl = request.getRequestURL();
                response.sendRedirect("http://passport.gmall.com:8088/index?ReturnUrl="+ReturnUrl);
                return false;
            }
            request.setAttribute("memberId",successMap.get("memberId"));
            request.setAttribute("nickname",successMap.get("nickname"));
            //token验证成功，写入cookie
            CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
        }else{
            //loginRequired为false的时候
           if("success".equals(success)){
               //如果token验证成功
               request.setAttribute("memberId",successMap.get("memberId"));
               request.setAttribute("nickname",successMap.get("nickname"));
               CookieUtil.setCookie(request,response,"oldToken",token,60*60*2,true);
           }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {

    }


}
