package cn.itcast.hotel.service;

import cn.itcast.hotel.pojo.Hotel;
import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.pojo.pageResult;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IHotelService extends IService<Hotel> {
    pageResult list(RequestParams requestParams);

    pageResult suggestion(RequestParams requestParams);
}
