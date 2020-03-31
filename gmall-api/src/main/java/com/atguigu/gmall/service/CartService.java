package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;

import java.util.List;

/**
 * @author yangkun
 * @date 2020/3/2
 */
public interface CartService {
    OmsCartItem ifCartExistByUser(String memberId, String skuId);

    void addCart(OmsCartItem omsCartItem);

    void updateCart(OmsCartItem omsCartItem);

    void flushCache(String memberId);

    List<OmsCartItem> getCartList(String memberId);

    void checkCart(OmsCartItem omsCartItem);

    void delCart(OmsOrderItem omsOrderItem);

    void saveCookieValue(List<OmsCartItem> omsCartItems,String memberId);

    void createCookieQueue(List<OmsCartItem> omsCartItems, String memberId);
}
