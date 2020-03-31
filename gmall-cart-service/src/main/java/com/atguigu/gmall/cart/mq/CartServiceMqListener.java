package com.atguigu.gmall.cart.mq;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.service.CartService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.List;

/**
 * @author yangkun
 * @date 2020/3/30
 */
@Component
public class CartServiceMqListener {
    @Autowired
    CartService cartService;
    @JmsListener(destination = "CART_COOKIE_QUEUE",containerFactory = "jmsQueueListener")
    public void consumeCookie(MapMessage message){
        try {
            String memberId = message.getString("memberId");
            String cookies = message.getString("omsCartItems");
            if(StringUtils.isNotBlank(cookies)&&StringUtils.isNotBlank(memberId)){
                List<OmsCartItem>  omsCartItems = JSON.parseArray(cookies, OmsCartItem.class);
                //保存cookie
                cartService.saveCookieValue(omsCartItems,memberId);
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
