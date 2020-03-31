package com.atguigu.gmall.payment.mq;


import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Date;
import java.util.Map;

/**
 * @author yangkun
 * @date 2020/3/23
 */

/**
 * 这个mq是为了实现对支付宝交易查询接口的
 * 延迟性队列消费者
 */
@Component
public class PaymentServiceMqListener {

    @Autowired
    PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_CHECK_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePayment(MapMessage mapMessage) throws JMSException {
        int count =mapMessage.getInt("count");
      String outTradeNo = mapMessage.getString("outTradeNo");
      //调用paymentService进行延迟性检查
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderSn(outTradeNo);
        //调用支付宝查询接口
        System.out.println("进行延迟检查，调用支付检查的服务接口");
        Map<String,Object> resultMap = paymentService.checkAlipayPayment(paymentInfo);
        if(resultMap!=null&&!resultMap.isEmpty()){
            String trade_status =(String) resultMap.get("trade_status");
            if (StringUtils.isNotBlank(trade_status)&&trade_status.equals("TRADE_SUCCESS")){
                System.out.println("支付成功修改支付信息，发送支付成功队列");
                PaymentInfo paymentInfoUpdate = new PaymentInfo();
                paymentInfoUpdate.setOrderSn((String) resultMap.get("out_trade_no"));//自己的订单号
                paymentInfoUpdate.setPaymentStatus("已支付");
                paymentInfoUpdate.setCallbackTime(new Date());
                paymentInfoUpdate.setAlipayTradeNo((String)resultMap.get("trade_no"));//支付宝中的订单号
                paymentInfoUpdate.setCallbackContent((String)resultMap.get("call_back_content"));
                paymentService.updatePaymentInfo(paymentInfoUpdate);
                //记得如果成功就直接返回
                return;
            }
        }
        if(count>0){
            // 继续发送延迟检查任务，计算延迟时间等
            System.out.println("没有支付成功，检查剩余次数为"+count+",继续发送延迟检查任务");
            count--;
            paymentService.sendDelayPaymentResultCheckQueue(outTradeNo,count);
        }else{
            System.out.println("检查剩余次数用尽，结束检查");
        }
    }
}
