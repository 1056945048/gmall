package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.service.SpuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yangkun
 * @date 2020/2/27
 */
@Controller
public class ItemController {
    @Reference
    SkuService skuService;
    @Reference
    SpuService spuService;
    /**
     * 用来测试thymeleaf用的
     * @param model
     * @return
     */
    @RequestMapping("index")
    public String index(Model model){
        model.addAttribute("hello","你好");
        List<String> list = new ArrayList<>();
        for(int i=0;i<5;i++){
            list.add("数据循环"+i);
        }
        model.addAttribute("list",list);
        model.addAttribute("check",0);
        return "index";
    }
    @RequestMapping("{skuId}.html")
    public String item(@PathVariable("skuId") String skuId ,Model model){
        PmsSkuInfo pmsSkuInfo = skuService.getSkuById(skuId);
        model.addAttribute("skuInfo",pmsSkuInfo);
        //得到此sku上的spu的所有属性
        List<PmsProductSaleAttr> pmsProductSaleAttrs = spuService.spuSaleAttrListCheckBySku(pmsSkuInfo.getProductId(), skuId);
        model.addAttribute("spuSaleAttrListCheckBySku",pmsProductSaleAttrs);

        /**
         * 通过此sku查询它的兄弟sku
         * 查询当前sku的spu的其他sku的集合的hash表
         */
        Map<String,String> skuSaleAttrMap = new HashMap<>();
        List<PmsSkuInfo> pmsSkuInfos = skuService.getSkuSaleAttrValueListBySpu(pmsSkuInfo.getProductId());
        for(PmsSkuInfo sku:pmsSkuInfos){
            String v=sku.getId();
            String k="";
            List<PmsSkuSaleAttrValue> skuSaleAttrValueList = sku.getSkuSaleAttrValueList();
            for(PmsSkuSaleAttrValue pmsSkuSaleAttrValue:skuSaleAttrValueList){
                //"239|245|"
                k += pmsSkuSaleAttrValue.getSaleAttrValueId() + "|";
            }
            skuSaleAttrMap.put(k,v);
        }
        //将sku的销售属性hash表放到页面
        String skuSaleAttrHashJson = JSON.toJSONString(skuSaleAttrMap);
        model.addAttribute("skuSaleAttrHashJson",skuSaleAttrHashJson);
        return "item";
    }
}
