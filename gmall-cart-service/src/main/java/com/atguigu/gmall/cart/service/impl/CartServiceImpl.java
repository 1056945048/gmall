package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.cart.mapper.OmsCartItemMapper;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yangkun
 * @date 2020/3/2
 */
@Service
public class CartServiceImpl implements CartService {
    @Autowired
    OmsCartItemMapper omsCartItemMapper;
    @Autowired
    RedisUtil redisUtil;
    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public OmsCartItem ifCartExistByUser(String memberId, String skuId) {
        Example e =new Example(OmsCartItem.class);
        e.createCriteria().andEqualTo("memberId",memberId).andEqualTo("productSkuId",skuId);
        OmsCartItem omsCartItem = omsCartItemMapper.selectOneByExample(e);
        return omsCartItem;
    }

    @Override
    public void addCart(OmsCartItem omsCartItem) {
        omsCartItemMapper.insertSelective(omsCartItem);
    }

    @Override
    public void updateCart(OmsCartItem omsCartItemFromDB) {
        Example example = new Example(OmsCartItem.class);
        example.createCriteria().andEqualTo("id",omsCartItemFromDB.getId());
        omsCartItemMapper.updateByExample(omsCartItemFromDB,example);
    }

    @Override
    public void flushCache(String memberId) {



            OmsCartItem omsCartItem = new OmsCartItem();
            omsCartItem.setMemberId(memberId);
            List<OmsCartItem> omsCartItems = omsCartItemMapper.select(omsCartItem);

            // 同步到redis缓存中
            Jedis jedis = redisUtil.getJedis();
        Map<String,String> map = new HashMap<>();
            for (OmsCartItem cartItem : omsCartItems) {
                cartItem.setTotalPrice(cartItem.getPrice().multiply(cartItem.getQuantity()));
                map.put(cartItem.getProductSkuId(), JSON.toJSONString(cartItem));
            }

            jedis.del("user:"+memberId+":cart");
            jedis.hmset("user:"+memberId+":cart",map);

            jedis.close();
        }


    @Override
    public List<OmsCartItem> getCartList(String memberId) {
        Jedis jedis = null;
       //查询缓存
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        try{
            jedis = redisUtil.getJedis();
            List<String> carts = jedis.hvals("user:"+memberId+":cart");
            for(String cart:carts){
                //将String类型的集合转化为对象
                OmsCartItem omsCartItem = JSON.parseObject(cart,OmsCartItem.class);
                omsCartItems.add(omsCartItem);
            }
        }catch(Exception e){
            //异常捕获
           e.printStackTrace();
           return null;
        }finally {
            jedis.close();
        }
        return omsCartItems;
    }

    /**
     * 购物车页面点击商品选择按钮触发
     * @param omsCartItem
     */
    @Override
    public void checkCart(OmsCartItem omsCartItem) {

        Example e = new Example(OmsCartItem.class);

        e.createCriteria().andEqualTo("memberId",omsCartItem.getMemberId()).andEqualTo("productSkuId",omsCartItem.getProductSkuId());

        omsCartItemMapper.updateByExampleSelective(omsCartItem,e);

        //缓存同步
        flushCache(omsCartItem.getMemberId());
    }

    /**
     * 完成订单的保存后，将购物车中的订单信息删除
     * @param
     */
    @Override
    public void delCart(OmsOrderItem omsOrderItem) {
        //先删除数据库中的信息
        Example e = new Example(OmsCartItem.class);
        e.createCriteria().andEqualTo("productSkuId",omsOrderItem.getProductSkuId()).andEqualTo("memberId",omsOrderItem.getMemberId());
        omsCartItemMapper.deleteByExample(e);
        String memberId = omsOrderItem.getMemberId();
        //更新缓存
        flushCache(memberId);
    }

    /**
     * 将cookie中的信息和购物车信息合并
     * 可以采用消息队列来完成
     * @param omsCartItems
     */
    @Override
    public void saveCookieValue(List<OmsCartItem> omsCartItems,String memberId) {
        for (OmsCartItem omsCartItem : omsCartItems) {
            omsCartItem.setMemberId(memberId);
            omsCartItemMapper.insertSelective(omsCartItem);
            this.flushCache(memberId);
        }
    }

    /**
     * 创建cookie消息队列
     * @param omsCartItems
     * @param memberId
     */
    @Override
    public void createCookieQueue(List<OmsCartItem> omsCartItems,String memberId){
        ConnectionFactory connectionFactory = null;
        Connection connection=null;
        Session session=null;
        try{
            connection = connectionFactory.createConnection();
            //不开启事务
            session = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);
            //创建队列
            Queue queue = session.createQueue("CART_COOKIE_QUEUE");
            //创建生产者
            MessageProducer producer =  session.createProducer(queue);
            //创建信息
            MapMessage  message =session.createMapMessage();
            message.setString("omsCartItems",JSON.toJSONString(omsCartItems));
            message.setString("memberId",memberId);
            //发送信息
            producer.send(message);
            session.commit();
            session.close();
            producer.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
       

}



