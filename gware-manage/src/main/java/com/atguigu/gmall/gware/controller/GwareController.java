package com.atguigu.gmall.gware.controller;


import com.atguigu.gmall.gware.bean.WmsWareInfo;
import com.atguigu.gmall.gware.bean.WmsWareSku;
import com.atguigu.gmall.gware.service.GwareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @author yangkun
 * @date 2020/3/30
 */
@Controller
public class GwareController {
    @Autowired
    GwareService gwareService;

    @RequestMapping("/index")
    public String index(){
        return "index";
    }
    @RequestMapping("wareSkuListPage")
    public String wareSkuListPage(){
       return "wareSkuListPage";
    }
    @RequestMapping(value = "wareInfoList",method = RequestMethod.GET,produces = "application/json;charset=utf-8")
    @ResponseBody
    public List<WmsWareInfo> getWareInfoList(){
        //查询所有的仓库
        List<WmsWareInfo> wmsWareInfos = gwareService.getWareInfoList();
        return wmsWareInfos;
    }

    /**
     * 给某个仓库添加商品和库存
     * @return
     */
    @RequestMapping(value = "saveWareSku",method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Void> saveWareSku(WmsWareSku wmsWareSku){
         gwareService.addWareSku(wmsWareSku);
         return ResponseEntity.ok().build();
    }

    /**
     * 显示所有库存中的商品
     */
    @RequestMapping(value = "wareSkuList",method = RequestMethod.GET)
    @ResponseBody
    public List<WmsWareSku> getWareSkuList(){
        List<WmsWareSku> wmsWareSkus = gwareService.getWareSkuList();
        return wmsWareSkus;
    }
}
