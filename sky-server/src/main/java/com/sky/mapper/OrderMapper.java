package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单数据
     * @param order
     */
    void insert(Orders order);

    /**
     * 根据订单号和用户id查询订单
     */
    @Select("select * from orders where number = #{orderNumber} and user_id = #{userId}")
    Orders getByNumberAndUserId(String orderNumber, Long userId);

    /**
     * 修改订单信息
     */
    void update(Orders orders);


    /**
     * 分页查询
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);


    /**
     * 根据id查询订单
     */
    @Select("SELECT * FROM orders WHERE id = #{id}")
    Orders getById(Long id);

    /**
     * 订单和用户一起查询----- 用于删除订单
     */
    @Select("select * from orders where id = #{id} and user_id = #{userId}")
    Orders getByIdAndUserId(@Param("id") Long id , @Param("userId") Long userId);


    /**
     * 根据不同订单状态统计订单
     * @param status
     */
    @Select("select count(*) from orders where status = #{status}")
    Integer countStatus(Integer status);

    /**
     * 根据状态和下单时间查询订单
     * @param status
     * @param orderTime
     */
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrdertimeLT(Integer status, LocalDateTime orderTime);

    /**
     * 条件更新（CAS）：仅当订单仍是待付款状态时才置为已取消，供定时任务使用。
     * 若影响行数为 0，说明该订单已被并发修改（如用户刚支付成功），不能覆盖。
     */
    @Update("update orders set status = 6, cancel_reason = #{cancelReason}, cancel_time = #{cancelTime} " +
            "where id = #{id} and status = 1")
    int cancelTimeoutOrder(Orders orders);


    /**
     * 支付成功：仅当订单仍为待付款时更新，返回影响行数
     */
    @Update("update orders set status = #{status}, pay_status = #{payStatus}, checkout_time = #{checkoutTime} " +
            "where id = #{id} and status = 1")
    int paySuccess(Orders orders);


    /**
     * 根据动态条件统计营业额
     * @param map
     */
    Double sumByMap(Map map);

    /**
     * 根据动态条件统计订单数量
     * @param map
     */
    Integer countByMap(Map map);
}
