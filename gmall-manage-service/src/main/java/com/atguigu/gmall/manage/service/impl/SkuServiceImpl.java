package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsSkuAttrValue;
import com.atguigu.gmall.bean.PmsSkuImage;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * @author yangkun
 * @date 2020/2/27
 */
@Service
public class SkuServiceImpl implements SkuService {
    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;
    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;
    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;
    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;
    @Autowired
    RedisUtil redisUtil;
    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {
       int i = pmsSkuInfoMapper.insertSelective(pmsSkuInfo);
       String skuId = pmsSkuInfo.getId();
       //插入平台相关属性
       List<PmsSkuAttrValue> pmsSkuAttrValues = pmsSkuInfo.getSkuAttrValueList();
       for(PmsSkuAttrValue pmsSkuAttrValue:pmsSkuAttrValues){
           pmsSkuAttrValue.setSkuId(skuId);
           pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
       }
       //插入销售属性
        List<PmsSkuSaleAttrValue> pmsSkuSaleAttrValues = pmsSkuInfo.getSkuSaleAttrValueList();
       for(PmsSkuSaleAttrValue pmsSkuSaleAttrValue : pmsSkuSaleAttrValues){
           pmsSkuSaleAttrValue.setSkuId(skuId);
           pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);
       }
       //插入图片信息
        List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();
        for (PmsSkuImage pmsSkuImage : skuImageList) {
            pmsSkuImage.setSkuId(skuId);
            pmsSkuImageMapper.insertSelective(pmsSkuImage);
        }
        // 发出商品的缓存同步消息
        // 发出商品的搜索引擎的同步消息
    }
    public PmsSkuInfo getSkuByIdFromDd(String skuId){
        //得到商品信息
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(skuId);
        PmsSkuInfo sku = pmsSkuInfoMapper.selectOne(pmsSkuInfo);
        //得到商品图片信息
        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> pmsSkuImages = pmsSkuImageMapper.select(pmsSkuImage);
        sku.setSkuImageList(pmsSkuImages);
        return sku;
    }

    /**
     * 为防止缓存击穿，即某个时间有大量的高并发访问一个sku,此时设置分布式锁
     * @param skuId
     * @return
     */
    @Override
    public PmsSkuInfo getSkuById(String skuId) {
        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        //连接缓存
        Jedis jedis = redisUtil.getJedis();
        //查询缓存
        String skuKey = "sku"+skuId+":info";
        String skuJson = jedis.get(skuKey);
        if(StringUtils.isNotBlank(skuJson)){
            //缓存中有,将json字符串转换成对象
            pmsSkuInfo = JSON.parseObject(skuJson,PmsSkuInfo.class);
            jedis.close();
            return pmsSkuInfo;
        }else{
            //缓存中没有,查询mysql
            //设置分布式锁（缓存击穿---一个热点key失效）
            String token = UUID.randomUUID().toString();
            //设置拿到锁的时间有十秒钟过期时间

            String OK = jedis.set("sku"+skuId+":token",token,"nx","px",10*1000);
            if(StringUtils.isNotBlank(OK)&&OK.equals("OK")) {
                //设置成功，有10秒钟访问权限
                pmsSkuInfo = getSkuByIdFromDd(skuId);
                if (pmsSkuInfo != null) {
                    jedis.set("sku" + skuId + ":info", JSON.toJSONString(pmsSkuInfo));
                } else {
                    //为防止缓存穿透，将值设为null
                    jedis.setex("sku" + skuId + "info", 60 * 60 * 3, JSON.toJSONString(""));
                }
                //访问完成后将分布式锁释放
                String lockToken = jedis.get("sku" + skuId + ":token");
                if (StringUtils.isNotBlank(lockToken) && token.equals(lockToken)) {
                    //这里最好使用lua脚本，防止高并发时删除其他人的锁
                    jedis.del("sku" + skuId + ":token");
                }
            } else{
                //没有拿到分布式锁开始自旋
                System.out.println(Thread.currentThread().getName()+"没有拿到锁开始自旋");
                try{
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //加不加return是有区别的，会产生不同的线程
                return getSkuById(skuId);
            }
            jedis.close();
            return pmsSkuInfo;
        }
    }

    /**
     * 构造当前spuId下的hash表
     * @param productId
     * @return
     */
    @Override
    public List<PmsSkuInfo> getSkuSaleAttrValueListBySpu(String productId) {
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectSkuSaleAttrValueListBySpu(productId);
        return pmsSkuInfos;
    }

    @Override
    public List<PmsSkuInfo> getAllSku(String catalog3Id) {
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectAll();
        System.out.println(pmsSkuInfos);
        for(PmsSkuInfo skuInfo:pmsSkuInfos){
            PmsSkuAttrValue  pmsSkuAttrValue = new PmsSkuAttrValue();
            pmsSkuAttrValue.setSkuId(skuInfo.getId());
            List<PmsSkuAttrValue> pmsSkuAttrValues = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);
            skuInfo.setSkuAttrValueList(pmsSkuAttrValues);
        }
        return pmsSkuInfos;
    }

    @Override
    public boolean checkPrice(String productSkuId, BigDecimal price) {
        boolean b = false;
        PmsSkuInfo pmsSkuInfo =new PmsSkuInfo();
        pmsSkuInfo.setId(productSkuId);
        PmsSkuInfo pmsSkuInfo1 = pmsSkuInfoMapper.selectByPrimaryKey(pmsSkuInfo);
        BigDecimal skuPrice = pmsSkuInfo1.getPrice();
        if(price.compareTo(skuPrice)==0){
            b = true;
        }
        return b;
    }
}
