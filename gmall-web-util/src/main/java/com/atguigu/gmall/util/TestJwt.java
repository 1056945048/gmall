package com.atguigu.gmall.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.SimpleFormatter;

/**
 * @author yangkun
 * @date 2020/3/14
 */
public class TestJwt {


    public static void main(String[] args) {
        Map<String,Object> map=new HashMap<>();
        map.put("memberId","1");
        map.put("nickname","张三");
        String ip="127.0.0.1";
        String time=new SimpleDateFormat("yyyyMMdd HHmm").format(new Date());
        String encode = JwtUtil.encode("gmall201905", map, ip + time);
        System.out.println("encode:"+encode);

    }

 }
