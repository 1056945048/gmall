package com.atguigu.gmall.gware.service.impl;


import com.atguigu.gmall.gware.bean.WmsWareInfo;
import com.atguigu.gmall.gware.bean.WmsWareSku;
import com.atguigu.gmall.gware.mapper.WareInfoMapper;
import com.atguigu.gmall.gware.mapper.WareSkuMapper;
import com.atguigu.gmall.gware.service.GwareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author yangkun
 * @date 2020/3/30
 */
@Service
public class GwareServiceImpl implements GwareService {
    @Autowired
    WareInfoMapper wareInfoMapper;
    @Autowired
    WareSkuMapper wareSkuMapper;

    @Override
    public List<WmsWareInfo> getWareInfoList() {
        List<WmsWareInfo> wmsWareInfos = wareInfoMapper.selectAll();
        return wmsWareInfos;
    }

    @Override
    public void addWareSku(WmsWareSku wmsWareSku) {
        wareSkuMapper.insertSelective(wmsWareSku);
    }

    @Override
    public List<WmsWareSku> getWareSkuList() {
        List<WmsWareSku> wmsWareSkus = wareSkuMapper.selectAll();
        for(WmsWareSku wmsWareSku:wmsWareSkus){
            String warehouseId = wmsWareSku.getWarehouseId();
            WmsWareInfo wmsWareInfo = wareInfoMapper.selectByPrimaryKey(warehouseId);
            wmsWareSku.setWarehouseName(wmsWareInfo.getName());
        }
        return wmsWareSkus;
    }
}
