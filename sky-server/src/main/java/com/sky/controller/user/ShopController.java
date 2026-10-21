package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.vo.ShopInfoVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Api(tags = "店铺相关接口")
@RestController("userShopController")
@RequestMapping("/user/shop")
public class ShopController {

    public static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${sky.shop.name:苍穹外卖}")
    private String shopName;

    @Value("${sky.shop.address:}")
    private String shopAddress;

    @Value("${sky.shop.phone:}")
    private String shopPhone;

    /**
     * 获取店铺信息（名称 / 地址 / 电话）
     * 小程序首页展示店铺地址，登录接口不返回这些字段，所以单独提供
     * @return
     */
    @GetMapping("/info")
    @ApiOperation("获取店铺信息")
    public Result<ShopInfoVO> getShopInfo(){
        ShopInfoVO shopInfoVO = ShopInfoVO.builder()
                .name(shopName)
                .address(shopAddress)
                .phone(shopPhone)
                .build();
        log.info("获取店铺信息：{}", shopInfoVO);
        return Result.success(shopInfoVO);
    }

    /**
     * 获取店铺的营业状态
     * @return
     */
    @GetMapping("/status")
    @ApiOperation("获取店铺的营业状态")
    public Result<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("获取到店铺的营业状态为：{}",(status != null && status == 1) ? "营业中" : "打烊中");
        return Result.success(status);
    }
}
