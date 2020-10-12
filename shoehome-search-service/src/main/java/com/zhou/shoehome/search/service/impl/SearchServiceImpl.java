package com.zhou.shoehome.search.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.zhou.shoehome.bean.PmsSearchParam;
import com.zhou.shoehome.bean.PmsSearchSkuInfo;
import com.zhou.shoehome.service.ISearchService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhouzh6
 */
@Service
public class SearchServiceImpl implements ISearchService {

    @Autowired
    JestClient jestClient;

    @Override
    public List<PmsSearchSkuInfo> list(PmsSearchParam pmsSearchParam) {

        String dslStr = getSearchDsl(pmsSearchParam);

        List<PmsSearchSkuInfo> pmsSearchSkuInfoList = new ArrayList<>();
        try {
            Search search = new Search.Builder(dslStr)
                    .addIndex("索引名")
                    .addType("表名")
                    .build();
            SearchResult execute = jestClient.execute(search);

            List<SearchResult.Hit<PmsSearchSkuInfo, Void>> hitList = execute.getHits(PmsSearchSkuInfo.class);
            for (SearchResult.Hit<PmsSearchSkuInfo, Void> hit : hitList) {
                PmsSearchSkuInfo skuInfo = hit.source;
                Map<String, List<String>> highlight = hit.highlight;
                if (highlight != null) {
                    String skuName = highlight.get("skuName").get(0);
                    skuInfo.setSkuName(skuName);
                }
                pmsSearchSkuInfoList.add(skuInfo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pmsSearchSkuInfoList;
    }

    private String getSearchDsl(PmsSearchParam pmsSearchParam) {
        // 创建查询表达式dsl
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // bool
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();

        // filter与term
        String catalog3Id = pmsSearchParam.getCatalog3Id();
        if (StringUtils.isNotBlank(catalog3Id)) {
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", catalog3Id);
            boolQueryBuilder.filter(termQueryBuilder);
        }
        String[] pmsSearchParamValueIds = pmsSearchParam.getValueId();
        if (pmsSearchParamValueIds.length > 0) {
            for (String pmsSearchParamValueId : pmsSearchParamValueIds) {
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", pmsSearchParamValueId);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }

        // must与match
        String keyword = pmsSearchParam.getKeyword();
        if (StringUtils.isNotBlank(keyword)) {
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", keyword);
            boolQueryBuilder.must(matchQueryBuilder);
        }

        // query
        searchSourceBuilder.query(boolQueryBuilder);
        // from
        searchSourceBuilder.from(0);
        // size
        searchSourceBuilder.size(20);
        // highlight
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red;'>");
        highlightBuilder.field("skuName");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlight(highlightBuilder);

        // sort
        searchSourceBuilder.sort("id", SortOrder.DESC);

        // aggs
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAtttValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);

        return searchSourceBuilder.toString();
    }
}
