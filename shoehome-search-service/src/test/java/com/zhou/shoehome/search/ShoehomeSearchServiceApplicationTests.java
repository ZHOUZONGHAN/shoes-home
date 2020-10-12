package com.zhou.shoehome.search;


import com.alibaba.dubbo.config.annotation.Reference;
import com.zhou.shoehome.bean.PmsSearchSkuInfo;
import com.zhou.shoehome.bean.PmsSkuInfo;
import com.zhou.shoehome.service.ISkuService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.beanutils.BeanUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ShoehomeSearchServiceApplicationTests {

    @Reference
    ISkuService skuService;

    @Autowired
    JestClient jestClient;

    @Test
    public void contextLoads() {
    }

    public void get() throws IOException {
        // 创建查询表达式dsl
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        // filter与term
        TermQueryBuilder termQueryBuilder = new TermQueryBuilder("", "");
        TermsQueryBuilder termsQueryBuilder = new TermsQueryBuilder("", "", "", "");
        boolQueryBuilder.filter(termQueryBuilder);
        // must与match
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("", "");
        boolQueryBuilder.must(matchQueryBuilder);
        // query
        searchSourceBuilder.query(boolQueryBuilder);
        // from
        searchSourceBuilder.from(0);
        // size
        searchSourceBuilder.size(20);
        // highlight
        searchSourceBuilder.highlight();

        String dslStr = searchSourceBuilder.toString();

        Search search = new Search.Builder(dslStr).addIndex("索引名").addType("表名").build();

        SearchResult execute = jestClient.execute(search);

        List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hitList = execute.getHits(PmsSearchSkuInfo.class);
        List<PmsSearchSkuInfo> pmsSearchSkuInfoList = new ArrayList<>();
        for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hitList) {
            PmsSearchSkuInfo skuInfo = hit.source;
            pmsSearchSkuInfoList.add(skuInfo);
        }
    }

    public void put() {
        // 从数据库中查询数据
        List<PmsSkuInfo> skuServiceAllSku = skuService.getAllSku();

        List<PmsSearchSkuInfo> pmsSearchSkuInfoList = new ArrayList<>();
        try {

            for (PmsSkuInfo pmsSkuInfo : skuServiceAllSku) {
                PmsSearchSkuInfo pmsSearchSkuInfo = new PmsSearchSkuInfo();
                // 翻译
                BeanUtils.copyProperties(pmsSkuInfo, pmsSearchSkuInfo);
                pmsSearchSkuInfo.setId(Long.parseLong(pmsSkuInfo.getId()));
                pmsSearchSkuInfoList.add(pmsSearchSkuInfo);
            }

            // 将数据导入es
            for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfoList) {
                Index index = new Index.Builder(pmsSearchSkuInfo)
                        .index("shoehome")
                        .type("PmsSkuInfo")
                        .id(String.valueOf(pmsSearchSkuInfo.getId()))
                        .build();
                jestClient.execute(index);
            }
        } catch (IOException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
