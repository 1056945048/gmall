package com.atguigu.gmall.manage.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.PmsBaseAttrInfo;
import com.atguigu.gmall.bean.PmsBaseAttrValue;
import com.atguigu.gmall.service.AttrInfoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @author yangkun
 * @date 2020/2/26
 */
@Controller
@CrossOrigin
public class AttrInfoController {
    @Reference
    AttrInfoService attrInfoService;

    @RequestMapping("attrInfoList")
    @ResponseBody
    public List<PmsBaseAttrInfo> attrInfoList(String catalog3Id){
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = attrInfoService.attrInfoList(catalog3Id);
        return pmsBaseAttrInfos;
    }
    //对应添加属性值按钮,属性和属性值都在这里添加
    @RequestMapping("saveAttrInfo")
    @ResponseBody
    public String saveAttrInfo(@RequestBody PmsBaseAttrInfo pmsBaseAttrInfo){
        String success = attrInfoService.saveAttrInfo(pmsBaseAttrInfo);
        return success;
    }
    //对应修改按钮
    @RequestMapping("getAttrValueList")
    @ResponseBody
    public List<PmsBaseAttrValue> getAttrValueList(String attrId){
        List<PmsBaseAttrValue> pmsBaseAttrValues = attrInfoService.getAttrValueList(attrId);
        return pmsBaseAttrValues;
    }

}
