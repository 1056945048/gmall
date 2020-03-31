package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.SkuService;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author yangkun
 * @date 2020/2/27
 */
@Controller
@CrossOrigin
public class SkuController {
    @Reference
    SkuService skuService;
    @RequestMapping("saveSkuInfo")
    @ResponseBody
    public String saveSkuInfo(@RequestBody PmsSkuInfo pmsSkuInfo){
         //将spuId封装给productId
         pmsSkuInfo.setProductId(pmsSkuInfo.getSpuId());
         //处理默认图片
         String defaultImg = pmsSkuInfo.getSkuDefaultImg();
         //如果没有默认图片
         if(!StringUtils.isEmpty(defaultImg)){
            pmsSkuInfo.setSkuDefaultImg(pmsSkuInfo.getSkuImageList().get(0).getImgUrl());
         }
         skuService.saveSkuInfo(pmsSkuInfo);
         return "success";
    }
}
