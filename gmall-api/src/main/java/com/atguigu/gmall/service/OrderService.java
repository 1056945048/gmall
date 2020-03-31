package com.atguigu.gmall.service;


import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;

/**
 * @author yangkun
 * @date 2020/3/19
 */
public interface OrderService {
    String geneTradeCode(String memberId);

    String checkTradeCode(String memberId,String tradeCode);


    void saveOrder(OmsOrder omsOrder);


    OmsOrder getOrderInfo(String outTradeNo);

    void updateOrder(OmsOrder omsOrder);
}
