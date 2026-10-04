package com.sky.service;

import com.sky.dto.SetmealDTO;

public interface SetmealService {

    /**
     * 新增套餐和套餐包含的菜品
     * @param setmealDTO
     */
    void saveWithDish(SetmealDTO setmealDTO);

}
