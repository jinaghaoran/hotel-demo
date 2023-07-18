package cn.itcast.hotel.service.impl;

import cn.itcast.hotel.mapper.HotelMapper;
import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.HotelDoc;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.pojo.pageResult;
import cn.itcast.hotel.service.IHotelService;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author jhr
 */
@Service
public class HotelServiceImpl extends ServiceImpl<HotelMapper, Hotel> implements IHotelService {
    @Resource
    private RestHighLevelClient client;

    @Override
    public pageResult list(RequestParams requestParams) {
        try {
            SearchRequest request = new SearchRequest("hotel");
            // 2.准备请求参数,dsl语句,match 效果
//            使用bool查询，实现多个条件查询:
//            可选：地市：city，星级：starName,品牌：brand,价格：minPrice至maxPrice
//            boolean 查询
            BoolQueryBuilder boolQueryBuilder = getBoolQueryBuilder(requestParams);
//            实现算分：算分控制，即钱面的广告
            FunctionScoreQueryBuilder scoreQueryBuilder = QueryBuilders.functionScoreQuery(
                    boolQueryBuilder,
                    new FunctionScoreQueryBuilder.FilterFunctionBuilder[]{
//                            其中的一个functionScore元素
                            new FunctionScoreQueryBuilder.FilterFunctionBuilder(
                                    QueryBuilders.termQuery("isAD", true),
                                    ScoreFunctionBuilders.weightFactorFunction(10)
                            )
                    }
            );
            request.source().query(scoreQueryBuilder)
//                分页与排序，与query同级别
                    .from((requestParams.getPage() - 1) * requestParams.getSize())
                    .size(requestParams.getSize());

            //            默认进行距离排序，返回距离最近的酒店
            String location = requestParams.getLocation();
            System.out.println("locationParams:" + location);
            if (!StringUtils.isBlank(location)) {
                request.source().sort(
                        SortBuilders.geoDistanceSort("location",
                                        new GeoPoint(location))
                                .order(SortOrder.ASC)
                                .unit(DistanceUnit.KILOMETERS)   //km的距离 格式数据
                );
            }

            if (!StringUtils.equals("default", requestParams.getSortBy())) {
                request.source().sort(requestParams.getSortBy(), SortOrder.DESC);
            }
            return handleResponse(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static BoolQueryBuilder getBoolQueryBuilder(RequestParams requestParams) {
        BoolQueryBuilder queryBuilder = new BoolQueryBuilder();

        if (StringUtils.isBlank(requestParams.getKey())) {
//                request.source().query(
//                        QueryBuilders.matchAllQuery()
//                );
//                 替换为boolQuery
            queryBuilder.must(
                    QueryBuilders.matchAllQuery()
            );
        } else {
//                request.source().query(
//                        QueryBuilders.matchQuery("all", requestParams.getKey())
//                );
            queryBuilder.must(
                    QueryBuilders.matchQuery("all", requestParams.getKey())
            );
        }
//          城市
        if (StringUtils.isNotBlank(requestParams.getCity())) {
            queryBuilder.filter(QueryBuilders.termQuery("city", requestParams.getCity()));
        }

        //          星级
        if (StringUtils.isNotBlank(requestParams.getStarName())) {
            queryBuilder.filter(QueryBuilders.termQuery("starName", requestParams.getStarName()));
        }

//            品牌：brand
        if (StringUtils.isNotBlank(requestParams.getBrand())) {
            queryBuilder.filter(QueryBuilders.termsQuery("brand", requestParams.getBrand()));
        }

//            价格：minPrice至maxPrice
        if (StringUtils.isNotBlank(requestParams.getMinPrice()) && StringUtils.isNotBlank(requestParams.getMaxPrice())) {
            queryBuilder.filter(QueryBuilders
                    .rangeQuery("price")
                    .gt(requestParams.getMinPrice())
                    .lt(requestParams.getMaxPrice())
            );

        }
        return queryBuilder;
    }

    @Override
    public pageResult suggestion(RequestParams requestParams) {
        return null;
    }

    private pageResult handleResponse(SearchRequest request) throws IOException {
        // 3.发送请求
        SearchResponse search = client.search(request, RequestOptions.DEFAULT);
        // 结果打印
        System.out.println(search);
        //处理结果
        SearchHits hits = search.getHits();
        //获取总条数
        pageResult pageResult = new pageResult();
        ArrayList<HotelDoc> hotelDocs = new ArrayList<>();
        long total = hits.getTotalHits().value;
        System.out.println("总条数：" + total);
        SearchHit[] hitsHits = hits.getHits();
        for (SearchHit hit : hitsHits) {
            String sourceAsString = hit.getSourceAsString();
            //fastJson 反序列化
            HotelDoc hotelDoc = JSON.parseObject(sourceAsString, HotelDoc.class);
//            获取排序值
            Object[] sortValues = hit.getSortValues();
            System.out.println("sortValues" + Arrays.toString(sortValues));
            if (!ObjectUtils.isEmpty(sortValues)) {
                hotelDoc.setDistance(sortValues[0]);
            }
            hotelDocs.add(hotelDoc);
            System.out.println("hotelDoc:" + hotelDoc + "\n");
        }
        pageResult.setHotels(hotelDocs);
        pageResult.setTotal(total);
        return pageResult;
    }
}
