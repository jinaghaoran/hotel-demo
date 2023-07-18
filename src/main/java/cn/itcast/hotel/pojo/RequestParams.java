package cn.itcast.hotel.pojo;

import lombok.Data;

/**
 * RequestParams
 *
 * @Author jhr
 * @Date 2023/7/16
 */
@Data
public class RequestParams {
    private String key;
    private Integer page;
    private Integer size;
    private String sortBy;

    private String name;
    private String address;
    private Integer price;
    private Integer score;
    private String brand;
    private String city;
    private String starName;
    private String business;
    private String longitude;
    private String latitude;
    private String pic;

//    价格区间：minPrice至maxPrice
    private String minPrice;
    private String maxPrice;

    private String location;
}
