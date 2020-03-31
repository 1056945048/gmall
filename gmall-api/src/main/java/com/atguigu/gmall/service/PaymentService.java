package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

import java.util.Map;

/**
 * @author yangkun
 * @date 2020/3/22
 */
public interface PaymentService {
    void savePaymentInfo(PaymentInfo paymentInfo);


    void updatePaymentInfo(PaymentInfo paymentInfo);

    void sendDelayPaymentResultCheckQueue(String outTradeNo,int count);

    Map<String, Object> checkAlipayPayment(PaymentInfo paymentInfo) ;
}
