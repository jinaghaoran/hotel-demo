package cn.itcast.hotel;

import cn.itcast.hotel.pojo.HotelDoc;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.swing.text.Highlighter;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static cn.itcast.hotel.constants.HotelIndexConstants.MAPPING_TEMPLATE;

@SpringBootTest
class HotelRestClientTest {

    private RestHighLevelClient client;

    @Test
    void testMatchAll() throws IOException {
        // 1.准备Request     es中是： GET /hotel/_search
        SearchRequest request = new SearchRequest("hotel");
        // 2.准备请求参数,dsl语句,match_all 效果
        request.source().query(QueryBuilders.matchAllQuery());
        // 3.发送请求
        handleResponse(request);

    }

    @Test
    void testMatch() throws IOException {
        // 1.准备Request     es中是： GET /hotel/_search
        SearchRequest request = new SearchRequest("hotel");
        // 2.准备请求参数,dsl语句,match 效果
        request.source().query(
                QueryBuilders.matchQuery("all", "如家")
        );
        handleResponse(request);

    }

    @Test
    void testTermQuery() throws IOException {
        // 1.准备Request     es中是： GET /hotel/_search
        SearchRequest request = new SearchRequest("hotel");
        // 2.准备请求参数,dsl语句,词条查询
//        request.source().query(
//                QueryBuilders.termQuery("city","上海")
//        );
//        价格大于等于100，小于等于500
        request.source().query(
                QueryBuilders.rangeQuery("price")
                        .gte(100)
                        .lte(500)
        );
        handleResponse(request);

    }

    @Test
    void testBoolQuery() throws IOException {
        // 1.准备Request     es中是： GET /hotel/_search
        SearchRequest request = new SearchRequest("hotel");
        // 2.准备请求参数,dsl语句,词条查询
//        request.source().query(
//                QueryBuilders.termQuery("city","上海")
//        );
//        创建布尔查询，
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
//        添加条件
        boolQuery.must(
                //                必须匹配查询，类似于 与，参与算分
                QueryBuilders.termQuery("city", "上海")
        ).filter(
//                        必须匹配，不参与算分，
                QueryBuilders.rangeQuery("price").lte(500)
        ).mustNot(
//                        必须不匹配，不参与算分，类似于非
                QueryBuilders.termQuery("name", "7天")
        ).should(
//                        选择性匹配查询，类似于或
                QueryBuilders.termQuery("city", "杭州")
        )

        ;

        request.source()
                .query(boolQuery)
//                分页与排序，与query同级别
                .from(10)
                .size(100)
                .sort("price", SortOrder.DESC);
        handleResponse(request);

    }


    @Test
    void testQueryPageAndSort() throws IOException {

        // 1.准备Request     es中是： GET /hotel/_search
        SearchRequest request = new SearchRequest("hotel");
        // 2.准备请求参数,dsl语句,词条查询
//        模拟业务中的分页，前端返回，pageNum,pageSize,
        int pageNum = 1;
        int pageSize = 20;
        request.source()
                .query(
                        QueryBuilders.termQuery("city", "上海")
                )
                //                分页与排序，与query同级别
                .from((pageNum - 1) * pageSize)
                .size(100)
                .sort("price", SortOrder.DESC);
        handleResponse(request);

    }

    @Test
    void testHighLight() throws IOException {
        // 1.准备Request     es中是： GET /hotel/_search
        SearchRequest request = new SearchRequest("hotel");
        // 2.准备请求参数,dsl语句,词条查询
        request.source().query(
                QueryBuilders.termQuery("city","上海")
        );
        request.source().highlighter(
                new HighlightBuilder()
                        .field("name")
                        .field("city")
                        .preTags("<p>")
                        .postTags("</p>")
                        .requireFieldMatch(false)
        );
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
//        对于高亮结果需要自己处理，因为返回值格式比之前的不同，会有单独的highlight字段
        for (SearchHit hit : search.getHits()) {
            String sourceAsString = hit.getSourceAsString();
            //fastJson 反序列化
            HotelDoc hotelDoc = JSON.parseObject(sourceAsString, HotelDoc.class);
            Map<String, HighlightField> fields = hit.getHighlightFields();
            if (ObjectUtils.isNotEmpty(fields)){
//                根据字段获取高亮结果
                HighlightField name = fields.get("name");
                HighlightField cityH = fields.get("city");
                if (ObjectUtils.isNotEmpty(name)){
//                    根据高亮结果，获取值
                    String string = name.getFragments()[0].string();
                    System.out.println(string);
                    hotelDoc.setName(string);
                }
                if (ObjectUtils.isNotEmpty(cityH)){
//                    根据高亮结果，获取值
                    String city = cityH.getFragments()[0].string();
                    System.out.println(city);
                    hotelDoc.setCity(city);
                }
            }
            System.out.println("hotelDoc:"+hotelDoc+"\n");
        }
//        handleResponse(request);

    }

    /**
     * 方法抽取，结果处理类
     *
     * @param request
     * @throws IOException
     */
    private void handleResponse(SearchRequest request) throws IOException {
        // 3.发送请求
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        // 结果打印
        System.out.println(search);
        //处理结果
        SearchHits hits = search.getHits();
        //获取总条数
        long total = hits.getTotalHits().value;
        System.out.println("总条数：" + total);
        SearchHit[] hitsHits = hits.getHits();
        for (SearchHit hit : hitsHits) {
            String sourceAsString = hit.getSourceAsString();
            //fastJson 反序列化
            HotelDoc hotelDoc = JSON.parseObject(sourceAsString, HotelDoc.class);

            System.out.println("hotelDoc:" + hotelDoc + "\n");

        }
    }


    @BeforeEach
    void setUp() {
        client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://127.0.0.1:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        client.close();
    }


}
