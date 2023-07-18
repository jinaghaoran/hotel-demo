package cn.itcast.hotel.controller;

import cn.itcast.hotel.pojo.RequestParams;
import cn.itcast.hotel.pojo.pageResult;
import cn.itcast.hotel.service.IHotelService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * HotelDocCotroller
 *
 * @Author jhr
 * @Date 2023/7/16
 */
@RestController
@RequestMapping("/hotel")
public class HotelDocController {

    @Resource
    private IHotelService iHotelService;
    @PostMapping("/list")
    public pageResult list(@RequestBody RequestParams requestParams){
        System.out.println("requestParams"+requestParams);
        if (requestParams.getPage()==0||requestParams.getSize()==0){
            requestParams.setPage(1);
            requestParams.setSize(10);
        }
        return iHotelService.list(requestParams);
    }

    @GetMapping("/suggestion")
    public pageResult suggestion(RequestParams requestParams){
        return iHotelService.suggestion(requestParams);
    }
}
