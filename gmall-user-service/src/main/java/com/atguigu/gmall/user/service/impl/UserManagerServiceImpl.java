package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.UserService;
import com.atguigu.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.atguigu.gmall.user.mapper.UserMapper;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

/**
 * @author yangkun
 * @date 2020/3/13
 */
@Service
public class UserManagerServiceImpl implements UserService {
    @Autowired
    UserMapper userMapper;

    @Autowired
    UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;
    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<UmsMember> getAllUser() {

        List<UmsMember> umsMembers = userMapper.selectAll();//userMapper.selectAllUser();

        return umsMembers;
    }

    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {

        // 封装的参数对象
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);


        //        Example example = new Example(UmsMemberReceiveAddress.class);
        //        example.createCriteria().andEqualTo("memberId",memberId);
        //         List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberReceiveAddressMapper.selectByExample(example);

        return umsMemberReceiveAddresses;
    }
//-----------------------------------------
    /***
     * 前面几个是测试dubbo时使用的，这个用来判断用户的登陆
     * @param umsMember
     */
    @Override
    public UmsMember login(UmsMember umsMember) {
        Jedis jedis = null;
        try{
           jedis = redisUtil.getJedis();
           //连接缓存查询
           if(jedis != null){
               System.out.println(umsMember);
               String umsMemberStr = jedis.get("user:"+umsMember.getUsername()+umsMember.getPassword()+":info");
               if(StringUtils.isNotBlank(umsMemberStr)){
                   //用户信息正确
                   UmsMember umsMemberFromRedis = JSON.parseObject(umsMemberStr,UmsMember.class);
                   return umsMemberFromRedis;
               }
           }
           //连接redis失败,查询数据库
           UmsMember umsMemberFromDb = loginFromDb(umsMember);
           System.out.println(umsMemberFromDb);
           //将用户信息放入redis缓存
           if(umsMemberFromDb!=null){
               jedis.setex("user:"+umsMember.getUsername()+umsMember.getPassword()+":info",60*60*2,JSON.toJSONString(umsMemberFromDb));
           }
           return umsMemberFromDb;
        }finally{
            jedis.close();
        }

    }

    /**
     * 将生成的token保存一份在redis
     * @param token
     */
    @Override
    public void addTokenCache(String token,String memberId) {
        Jedis jedis=redisUtil.getJedis();

        jedis.setex("user:"+memberId+":token",60*60*2,token);

        jedis.close();
    }

    @Override
    public UmsMember checkOauthUser(UmsMember umsMemberCheck) {
        UmsMember umsMember = userMapper.selectOne(umsMemberCheck);
        return umsMember;
    }

    @Override
    public UmsMember addOauthUser(UmsMember umsMember) {
        userMapper.insertSelective(umsMember);
        return umsMember;
    }

    /**
     * 生成订单信息后填写收货人地址
     * @param receiveAddressId
     * @return
     */
    @Override
    public UmsMemberReceiveAddress getReceiceAddress(String receiveAddressId) {
        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setId(receiveAddressId);
        UmsMemberReceiveAddress orderUserAddress = umsMemberReceiveAddressMapper.selectOne(umsMemberReceiveAddress);
        return orderUserAddress;
    }

    /**
     * 根据数据库验证用户信息
     * 只根据username,password
     * @param umsMember
     * @return
     */
    public UmsMember loginFromDb(UmsMember umsMember){
        Example e =new Example(UmsMember.class);
        e.createCriteria().andEqualTo("username",umsMember.getUsername()).andEqualTo("password",umsMember.getPassword());
        UmsMember umsMemberfromDb = userMapper.selectOneByExample(e);
        return umsMemberfromDb;
    }
}
