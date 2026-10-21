package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 店铺信息（小程序首页 / 下单页展示用）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopInfoVO implements Serializable {

    /** 店铺名称 */
    private String name;

    /** 店铺地址 */
    private String address;

    /** 联系电话 */
    private String phone;
}
