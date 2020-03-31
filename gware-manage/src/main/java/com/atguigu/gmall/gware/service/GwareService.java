package com.atguigu.gmall.gware.service;

import com.atguigu.gmall.gware.bean.WmsWareInfo;
import com.atguigu.gmall.gware.bean.WmsWareSku;

import java.util.List;

/**
 * @author yangkun
 * @date 2020/3/30
 */
public interface GwareService {
    List<WmsWareInfo> getWareInfoList();

    void addWareSku(WmsWareSku wmsWareSku);

    List<WmsWareSku> getWareSkuList();
}
