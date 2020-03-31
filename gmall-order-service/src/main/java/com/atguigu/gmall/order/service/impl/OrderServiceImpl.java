package com.atguigu.gmall.order.service.impl;


import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsOrderMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.ActiveMQMessageProducer;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author yangkun
 * @date 2020/3/19
 */
@Service
public class OrderServiceImpl implements OrderService {
    @Reference
    CartService cartService;
    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;
    @Autowired
    OmsOrderMapper omsOrderMapper;
    @Autowired
    RedisUtil redisUtil;

    @Override
    public String geneTradeCode(String memberId) {



            Jedis   jedis = redisUtil.getJedis();
            String tradeKey= "user:"+memberId+":tradeCode";
            String tradeCode = UUID.randomUUID().toString();
            jedis.setex(tradeKey,60*60*2,tradeCode);
            jedis.close();

        return tradeCode;
    }

    @Override
    public String checkTradeCode(String memberId,String tradeCode) {
        Jedis jedis = null;
        try{
            jedis = redisUtil.getJedis();
            String tradeKey = "user:"+memberId+":tradeCode";
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Long eval = (Long)jedis.eval(script,Collections.singletonList(tradeKey),Collections.singletonList(tradeCode));
            if(eval!=null&eval!=0){
                jedis.del(tradeKey);
                return "success";
            }else{
                return "fail";
            }
        }finally{
            jedis.close();
        }
    }

    @Override
    public void saveOrder(OmsOrder omsOrder) {
     omsOrderMapper.insertSelective(omsOrder);
        System.out.println(omsOrder);
      String orderId= omsOrder.getId();
      List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();
      for(OmsOrderItem omsOrderItem:omsOrderItems){
          omsOrderItem.setOrderId(orderId);
          //保存订单详情信息
          omsOrderItemMapper.insertSelective(omsOrderItem);
          //删除购物车中的信息
          cartService.delCart(omsOrderItem);
      }

    }

    @Override
    public OmsOrder getOrderInfo(String outTradeNo) {
        Example e = new Example(OmsOrder.class);
        e.createCriteria().andEqualTo("orderSn",outTradeNo);
        OmsOrder omsOrder= omsOrderMapper.selectOneByExample(e);
        return omsOrder;
    }

    /**
     * MQListener要调用的方法
     * 支付成功队列消息被订单服务消费
     * 同时订单服务又创建一个订单支付成功队列给库存服务
     * @param omsOrder
     */
    @Override
    public void updateOrder(OmsOrder omsOrder) {
        //因为这个传过来的参数并没有太多信息，只有orderSn
        Example e = new Example(OmsOrder.class);
        e.createCriteria().andEqualTo("orderSn",omsOrder.getOrderSn());
        omsOrder.setPayType("1");
        omsOrder.setStatus("1");
        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();
        Connection connection = null;
        Session session= null;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(true,Session.SESSION_TRANSACTED);//开启事务
        } catch (JMSException ex) {
            ex.printStackTrace();
        }
        try {
            Queue orderPayQueue = session.createQueue("ORDER_PAY_QUEUE");//创建队列
            MessageProducer producer = session.createProducer(orderPayQueue);//创建生产者
            TextMessage textMessage = new ActiveMQTextMessage();
            //查询订单的对象，转成json字符串，并存入ORDER_PAY_QUEUE队列
            //因为参数omsOrder并没有太多的信息，所以从数据库中将完整的order信息拿出来
            OmsOrder omsOrderInfo = omsOrderMapper.selectOne(omsOrder);
            //还要此订单对应的所有订单详情信息
            OmsOrderItem omsOrderItem = new OmsOrderItem();
            omsOrderItem.setOrderSn(omsOrderInfo.getOrderSn());
            List<OmsOrderItem> omsOrderselect = omsOrderItemMapper.select(omsOrderItem);
            omsOrderInfo.setOmsOrderItems(omsOrderselect);
            textMessage.setText(JSON.toJSONString(omsOrderInfo));
            //更改订单状态
            omsOrderMapper.updateByExampleSelective(omsOrder,e);
            producer.send(textMessage);//发送订单更新队列
            session.commit();
            session.close();
            connection.close();
            producer.close();
        } catch (JMSException ex) {
            ex.printStackTrace();
        }


    }


}
