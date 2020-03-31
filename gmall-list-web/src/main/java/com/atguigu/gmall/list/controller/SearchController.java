package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.AttrInfoService;
import com.atguigu.gmall.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;

/**
 * @author yangkun
 * @date 2020/3/1
 */
@Controller
public class SearchController {
    @Reference
    SearchService searchService;
    @Reference
    AttrInfoService attrInfoService;

    /**
     * 搜索结果的页面
     * @param pmsSearchParam
     * @param map
     * @return
     */
  //  @LoginRequired(loginSuccess = false)
    @RequestMapping("list.html")
    public String list(PmsSearchParam pmsSearchParam, ModelMap map){
        //调用搜索服务
       List<PmsSearchSkuInfo> skuLsInfoList = searchService.list(pmsSearchParam);
       map.put("skuLsInfoList",skuLsInfoList);
       //抽取检索结果所包含的所有平台属性---不重复set
        Set<String> valueIdSet = new HashSet<>();
        for(PmsSearchSkuInfo searchSkuInfo:skuLsInfoList){
            List<PmsSkuAttrValue> pmsSkuAttrValues = searchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue:pmsSkuAttrValues){
                String valueId = pmsSkuAttrValue.getValueId();
                valueIdSet.add(valueId);
            }
        }
       List<PmsBaseAttrInfo> pmsBaseAttrInfos = attrInfoService.getAttrValueListByValueId(valueIdSet);
       //对平台属性集合进一步处理,去掉当前条件中valueId所在的条件组
        String[] dslValueId = pmsSearchParam.getValueId();
        List<PmsSearchCrumb> pmsSearchCrumbs = new ArrayList<>();
        if(dslValueId !=null){
            for(String delId : dslValueId){
                //生成面包屑参数
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                pmsSearchCrumb.setValueId(delId);
                //设置面包屑的url
                pmsSearchCrumb.setUrlParam(getUrLParamCrumb(pmsSearchParam,delId));
                Iterator<PmsBaseAttrInfo> iterator = pmsBaseAttrInfos.iterator();
                while(iterator.hasNext()){
                    PmsBaseAttrInfo pmsBaseAttrInfo = iterator.next();
                    List<PmsBaseAttrValue> pmsBaseAttrValues = pmsBaseAttrInfo.getAttrValueList();
                    for(PmsBaseAttrValue pmsBaseAttrValue :pmsBaseAttrValues){
                        String id = pmsBaseAttrValue.getId();
                         if(delId.equals(id)){
                             //设置面包屑名称
                             pmsSearchCrumb.setValueName(pmsBaseAttrValue.getValueName());
                             pmsSearchCrumbs.add(pmsSearchCrumb);
                             iterator.remove();
                         }
                    }
                }

            }

        }
        map.put("attrList",pmsBaseAttrInfos);
        //面包屑参数值
        map.put("attrValueSelectedList",pmsSearchCrumbs);
        //请求的keyword
        String keyword = pmsSearchParam.getKeyword();
        if(StringUtils.isNotBlank(keyword)){
            map.put("keyword",keyword);
        }


        //每次请求的urlParam返回给页面
        String urlParam =getUrlParam(pmsSearchParam);
        map.put("urlParam",urlParam);
        return "list";
    }
    //设置面包屑对应的url
    public  String getUrLParamCrumb(PmsSearchParam pmsSearchParam, String delId) {
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String[] skuAttrValueList = pmsSearchParam.getValueId();

        String urlParam = "";

        if (StringUtils.isNotBlank(keyword)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "keyword=" + keyword;
        }

        if (StringUtils.isNotBlank(catalog3Id)) {
            if (StringUtils.isNotBlank(urlParam)) {
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "catalog3Id=" + catalog3Id;
        }

        if (skuAttrValueList != null) {
            for (String pmsSkuAttrValue : skuAttrValueList) {
                if (!pmsSkuAttrValue.equals(delId)) {
                    urlParam = urlParam + "&valueId=" + pmsSkuAttrValue;
                }
            }
        }
        return urlParam;
    }

    public String getUrlParam(PmsSearchParam pmsSearchParam) {
        String keyword = pmsSearchParam.getKeyword();
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        String[] skuAttrValueList = pmsSearchParam.getValueId();
        String urlParam = "";
        if(StringUtils.isNotBlank(keyword)){
            if(StringUtils.isNotBlank(urlParam)){
                urlParam = urlParam + "&";
            }
            urlParam = urlParam + "keyWord=" + keyword;
        }
        if(StringUtils.isNotBlank(catalog3Id)){
            if(StringUtils.isNotBlank(catalog3Id)){
                urlParam = urlParam +"&";
            }
            urlParam = urlParam + "catalog3Id=" + catalog3Id;
        }
        if(skuAttrValueList != null){
          for(String valueId:skuAttrValueList){
              urlParam = urlParam + "valueId=" + valueId;
          }
        }
        return urlParam;
    }

    @RequestMapping("index")
    @LoginRequired(loginSuccess = false)
    public String index(){
        return "index";
    }
}
