package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.SpuService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author yangkun
 * @date 2020/2/26
 */
@Service
public class SpuServiceImpl implements SpuService {
    @Autowired
    PmsProductInfoMapper pmsProductInfoMapper;
    @Autowired
    PmsBaseSaleAttrMapper pmsBaseSaleAttrMapper;
    @Autowired
    PmsProductImageMapper pmsProductImageMapper;
    @Autowired
    PmsProductSaleAttrMapper pmsProductSaleAttrMapper;
    @Autowired
    PmsProductSaleAttrValueMapper pmsProductSaleAttrValueMapper;
    @Override
    public List<PmsProductInfo> spuList(String catalog3Id) {
        PmsProductInfo pmsProductInfo = new PmsProductInfo();
        pmsProductInfo.setCatalog3Id(catalog3Id);
        List<PmsProductInfo> pmsProductInfos = pmsProductInfoMapper.select(pmsProductInfo);
        return pmsProductInfos;
    }

    @Override
    public List<PmsBaseSaleAttr> baseSaleAttrList() {
        List<PmsBaseSaleAttr> pmsBaseSaleAttrs = pmsBaseSaleAttrMapper.selectAll();
        return pmsBaseSaleAttrs;
    }

    @Override
    public void saveSpuInfo(PmsProductInfo pmsProductInfo) {
         //保存商品信息
        pmsProductInfoMapper.insertSelective(pmsProductInfo);
        //得到商品主键id
        String productId = pmsProductInfo.getId();
        //先保存商品图片
        List<PmsProductImage> pmsProductImages = pmsProductInfo.getSpuImageList();
        for(PmsProductImage pmsProductImage:pmsProductImages){
            pmsProductImage.setProductId(productId);
            pmsProductImageMapper.insertSelective(pmsProductImage);
        }
        //保存销售属性
        List<PmsProductSaleAttr> pmsProductSaleAttrs = pmsProductInfo.getSpuSaleAttrList();
        for(PmsProductSaleAttr pmsProductSaleAttr:pmsProductSaleAttrs){
            pmsProductSaleAttr.setProductId(productId);
            pmsProductSaleAttrMapper.insertSelective(pmsProductSaleAttr);
            //保存销售属性下的所有值
            List<PmsProductSaleAttrValue> pmsProductSaleAttrValues = pmsProductSaleAttr.getSpuSaleAttrValueList();
            for(PmsProductSaleAttrValue pmsproductSaleAttrValue:pmsProductSaleAttrValues){
                pmsproductSaleAttrValue.setProductId(productId);
                pmsProductSaleAttrValueMapper.insertSelective(pmsproductSaleAttrValue);
            }
        }
    }

    @Override
    public List<PmsProductSaleAttr> spuSaleAttrList(String spuId) {
        PmsProductSaleAttr pmsProductSaleAttr = new PmsProductSaleAttr();
        pmsProductSaleAttr.setProductId(spuId);
        //拿到此商品的所有销售属性
        List<PmsProductSaleAttr> pmsProductSaleAttrs = pmsProductSaleAttrMapper.select(pmsProductSaleAttr);
        for(PmsProductSaleAttr productSaleAttr:pmsProductSaleAttrs){
            PmsProductSaleAttrValue pmsProductSaleAttrValue = new PmsProductSaleAttrValue();
            //设置销售属性值的搜索条件
            pmsProductSaleAttrValue.setProductId(spuId);
            String saleAttrId = productSaleAttr.getSaleAttrId();
            /*
             *  销售属性id用的系统的字典表中id，不是销售属性表的主键
             */
            pmsProductSaleAttrValue.setSaleAttrId(saleAttrId);
            //拿到某销售属性下的所有值,例如颜色下面的黄色和黑色
            List<PmsProductSaleAttrValue> pmsProductSaleAttrValues = pmsProductSaleAttrValueMapper.select(pmsProductSaleAttrValue);
            //将查到的属性值保存再属性中
            productSaleAttr.setSpuSaleAttrValueList(pmsProductSaleAttrValues);
        }
        return pmsProductSaleAttrs;
    }

    @Override
    public List<PmsProductImage> spuImageList(String spuId) {
        PmsProductImage pmsProductImage = new PmsProductImage();
        pmsProductImage.setProductId(spuId);
        List<PmsProductImage> pmsProductImages = pmsProductImageMapper.select(pmsProductImage);
        return pmsProductImages;
    }

    /**
     * 通过productId得到此spu下的所有销售属性
     * @param productId
     */
    @Override
    public List<PmsProductSaleAttr> spuSaleAttrListCheckBySku(String productId,String skuId) {
//          PmsProductSaleAttr pmsProductSaleAttr = new PmsProductSaleAttr();
//          pmsProductSaleAttr.setProductId(productId);
//          List<PmsProductSaleAttr> pmsProductSaleAttrs = pmsProductSaleAttrMapper.select(pmsProductSaleAttr);
//          for(PmsProductSaleAttr pmsSaleAttr:pmsProductSaleAttrs){
//              PmsProductSaleAttrValue pmsProductSaleAttrValue = new PmsProductSaleAttrValue();
//              pmsProductSaleAttrValue.setProductId(productId);
//              pmsProductSaleAttrValue.setSaleAttrId(pmsSaleAttr.getSaleAttrId());
//              List<PmsProductSaleAttrValue> pmsSaleAttrValues = pmsProductSaleAttrValueMapper.select(pmsProductSaleAttrValue);
//              pmsSaleAttr.setSpuSaleAttrValueList(pmsSaleAttrValues);
//          }
//          return pmsProductSaleAttrs;
        List<PmsProductSaleAttr> pmsProductSaleAttrs = pmsProductSaleAttrMapper.selectSpuSaleAttrListCheckBySku(productId,skuId);
        return pmsProductSaleAttrs;
    }
}
