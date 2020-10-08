package com.zhou.shoehome.service;

import com.zhou.shoehome.bean.PmsBaseCatalog1;
import com.zhou.shoehome.bean.PmsBaseCatalog2;
import com.zhou.shoehome.bean.PmsBaseCatalog3;

import java.util.List;

/**
 * @author zhouzh6
 * @date 2020-10-04
 */
public interface ICatalogService {

    List<PmsBaseCatalog1> getCatalog1();

    List<PmsBaseCatalog2> getCatalog2(String catalog1Id);

    List<PmsBaseCatalog3> getCatalog3(String catalog2Id);
}
