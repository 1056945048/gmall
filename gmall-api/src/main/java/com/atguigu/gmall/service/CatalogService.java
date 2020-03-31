package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PmsBaseCatalog1;
import com.atguigu.gmall.bean.PmsBaseCatalog2;
import com.atguigu.gmall.bean.PmsBaseCatalog3;

import java.util.List;

/**
 * @author yangkun
 * @date 2020/2/25
 */
public interface CatalogService {
    List<PmsBaseCatalog3> getCatalog3(String catalog2Id);

    List<PmsBaseCatalog2> getCatalog2(String catalog1Id);

    List<PmsBaseCatalog1> getCatalog1();
}
