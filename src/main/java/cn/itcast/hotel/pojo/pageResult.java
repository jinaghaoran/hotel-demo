package cn.itcast.hotel.pojo;

import lombok.Data;

import java.util.List;

/**
 * pageResult
 *
 * @Author jhr
 * @Date 2023/7/16
 */
@Data
public class pageResult {
    Long total;
    List<HotelDoc> hotels;
}
