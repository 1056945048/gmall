package com.atguigu.gmall.order.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsOrder;

import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsOrderMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.ActiveMQUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.*;
import java.util.List;

/**
 * @author yangkun
 * @date 2020/3/23
 */
@Component
public class OrderServiceMqListener {
    @Autowired
    OmsOrderMapper omsOrderMapper;
    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;
    @Autowired
    OrderService orderService;
    @Autowired
    ActiveMQUtil activeMQUtil;

    /**
     * 支付成功队列创建后被订单服务消费
     * 即就是要修改已支付订单的状态
     * 然后向库存模块发出一个订单修改队列
     * @param mapMessage
     * @throws JMSException
     */
    @JmsListener(destination = "PAYMENT_SUCCESS_QUEUE",containerFactory = "jmsQueueListener")
    public void consumePaymentResult(MapMessage mapMessage) throws JMSException {
        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();
        Connection connection = null;
        Session session = null;
        String orderSn = mapMessage.getString("orderSn");
        if (StringUtils.isNotBlank(orderSn)) {
            OmsOrder omsOrder = new OmsOrder();
            omsOrder.setOrderSn(orderSn);
            //更改订单状态
            orderService.updateOrder(omsOrder);
            System.out.println("支付队列已被消费");
            try{
                connection = connectionFactory.createConnection();
                //开启事务
                session = connection.createSession(true, Session.SESSION_TRANSACTED);
                //创建一个队列
                Queue orderPayQueue = session.createQueue("ORDER_PAY_QUEUE");
                //创建一个生产者
                MessageProducer producer = session.createProducer(orderPayQueue);
                //创建消息
                TextMessage message = session.createTextMessage();
                //查询关于这个omsorder的信息和所有omsOrderItem的信息
                OmsOrder omsOrderParam = omsOrderMapper.selectOne(omsOrder);
                OmsOrderItem omsOrderItem = new OmsOrderItem();
                omsOrderItem.setOrderSn(omsOrderParam.getOrderSn());
                List<OmsOrderItem> omsOrderItems = omsOrderItemMapper.select(omsOrderItem);
                omsOrderParam.setOmsOrderItems(omsOrderItems);
                //将查询结果放入message中
                message.setText(JSON.toJSONString(omsOrderParam));
                producer.send(message);
                session.commit();
                session.close();
                producer.close();
                connection.close();
            }catch (JMSException e){
                e.printStackTrace();
                session.rollback();
            }
        }
    }
}
