package com.zhou.shoehome.service;

import com.zhou.shoehome.bean.PmsSearchParam;
import com.zhou.shoehome.bean.PmsSearchSkuInfo;

import java.util.List;

/**
 * @author zhouzh6
 */
public interface ISearchService {
    List<PmsSearchSkuInfo> list(PmsSearchParam pmsSearchParam);
}
