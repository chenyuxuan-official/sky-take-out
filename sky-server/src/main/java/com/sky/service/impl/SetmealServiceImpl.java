package com.sky.service.impl;

import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增套餐和套餐中包含的菜品
     * @param setmealDTO
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveWithDish(SetmealDTO setmealDTO){
        // 设置为停售
        setmealDTO.setStatus(StatusConstant.DISABLE);

        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        // 数据层插入数据
        setmealMapper.insert(setmeal);

        // 拿主键
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        for(SetmealDish setmealDish : setmealDishes){
            setmealDish.setSetmealId(setmeal.getId());
        }

        // 批量插入表
        setmealDishMapper.insertBatch(setmealDishes);

    }
}
