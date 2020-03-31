package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.util.ActiveMQUtil;

import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yangkun
 * @date 2020/3/22
 */
@Service
public class PaymentServiceImpl implements PaymentService {
    @Autowired
    AlipayClient alipayClient;
    @Autowired
    PaymentInfoMapper paymentInfoMapper;
    @Autowired
    ActiveMQUtil activeMQUtil;

    /**
     * 保存订单为未支付
     * paymentInfo
     * @param
     */
    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    /**
     * 支付成功消息队列产生
     * @param paymentInfo
     */
    @Override
    public void updatePaymentInfo(PaymentInfo paymentInfo) {
        //幂等性检查
        PaymentInfo paymentInfoParam = new PaymentInfo();
        paymentInfoParam.setOrderSn(paymentInfo.getOrderSn());
        PaymentInfo paymentInfoResult = paymentInfoMapper.selectOne(paymentInfoParam);
        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();
        Connection connection = null;
        Session session = null;
        if (StringUtils.isNotBlank(paymentInfoResult.getPaymentStatus()) && "已支付".equals(paymentInfoResult.getPaymentStatus())) {
            //幂等性检查发现已经修改过状态
            return ;
        }else{
            try {
                connection = connectionFactory.createConnection();
                connection.start();
                session = connection.createSession(true,Session.SESSION_TRANSACTED);//开启事务
            } catch (JMSException ex) {
                ex.printStackTrace();
            }
            try{
                //修改支付订单信息为已支付
                Example e = new Example(PaymentInfo.class);
                e.createCriteria().andEqualTo("orderSn",paymentInfo.getOrderSn());
                paymentInfoMapper.updateByExampleSelective(paymentInfo,e);
                //创建消息队列
                Queue paymentSuccessQueue = session.createQueue("PAYMENT_SUCCESS_QUEUE");
                MessageProducer producer = session.createProducer(paymentSuccessQueue);//生产端
                //TextMessage textMessage = new ActiveMQTextMessage() -- 字符串结构的文本
                MapMessage mapMessage= new ActiveMQMapMessage();//hash结构
                mapMessage.setString("orderSn",paymentInfo.getOrderSn());//构造信息队列
                producer.send(mapMessage);//发送支付成功消息队列
                //再orderServiceMq中被消费
                session.commit();
                producer.close();
                connection.close();
                session.close();
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    /**
     * 再订单跳转到支付页面之前
     * 发送一个延迟消息队列
     * 检查支付是否成功
     * @param outTradeNo
     */
    @Override
    public void sendDelayPaymentResultCheckQueue(String outTradeNo,int count) {
        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();
        Connection connection = null;
        Session session = null;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(true,Session.SESSION_TRANSACTED);
            //创建队列
            Queue paymentCheckQueue =session.createQueue("PAYMENT_CHECK_QUEUE");
            //创建生产者
            MessageProducer producer = session.createProducer(paymentCheckQueue);
            //设置消息持久化
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            MapMessage mapMessage = session.createMapMessage();
            mapMessage.setString("outTradeNo",outTradeNo);
            mapMessage.setInt("count",count);
            //加入延迟时间
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,10*1000);
            producer.send(mapMessage);
            session.commit();
            session.close();
            producer.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }


    }

    /**
     * 延迟性队列的查询接口
     * @param paymentInfo
     * @return
     */
    @Override
    public Map<String,Object> checkAlipayPayment(PaymentInfo paymentInfo)  {
        //结果封装参数
        Map<String,Object> resultMap= new HashMap<>();
        AlipayTradeQueryRequest alipayTradeQueryRequest = new AlipayTradeQueryRequest();
        //支付宝查询请求参数
        Map<String,Object> requestMap= new HashMap<>();
        requestMap.put("out_trade_no",paymentInfo.getOrderSn());
        alipayTradeQueryRequest.setBizContent(JSON.toJSONString(requestMap));

        AlipayTradeQueryResponse execute = null;
        try {
            execute = alipayClient.execute(alipayTradeQueryRequest);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(execute.isSuccess()){
          System.out.println("调用成功");
          resultMap.put("trade_no",execute.getTradeNo());//支付宝交易号
            resultMap.put("outTradeNo",execute.getOutTradeNo());//自己的订单号
            resultMap.put("trade_status",execute.getTradeStatus());//交易状态
           resultMap.put("call_back_content",execute.getMsg());
        }else{
            System.out.println("调用失败");
        }
      return resultMap;
    }

}



