package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO){
        //异常情况的处理（收货地址为空、超出配送范围、购物车为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 取得当前用户信息
        Long userId = BaseContext.getCurrentId();
        ShoppingCart  shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);

        //查询当前用户的购物车数据
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw  new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 构造订单数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,order);
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setUserId(userId);
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setPayStatus(Orders.UN_PAID);
        order.setOrderTime(LocalDateTime.now());

        //向订单表插入1条数据
        orderMapper.insert(order);

        // 向订单明细数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        }

        //向明细表插入n条数据
        orderDetailMapper.insertBatch(orderDetailList);

        //清理购物车中的数据
        shoppingCartMapper.deleteByUserId(userId);

        //封装返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();

        return orderSubmitVO;
    }




    /**
     * 订单支付（模拟：个人小程序没有商户号，跳过真微信支付，直接视为支付成功）
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        paySuccess(ordersPaymentDTO.getOrderNumber());
        return null;
    }


    /**
     * 支付成功，修改订单状态
     */
    public void paySuccess(String outTradeNo) {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        // 根据订单号和用户id查询订单
        Orders ordersDB = orderMapper.getByNumberAndUserId(outTradeNo, userId);
        // 根据订单id更新订单的状态、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }


    /**
     * 历史订单查询
     */
    @Override
    public PageResult pageQueryUser(Integer page, Integer pageSize, Integer status){
        // 设置分页参数
        PageHelper.startPage(page,pageSize);

        // 构造查询条件 (只差当前用户订单)
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        // 分页查询
        Page<Orders> pageResult = orderMapper.pageQuery(ordersPageQueryDTO);

        // 拼装VO （订单 + 菜品明细）
        List<OrderVO> list = new ArrayList<>();
        if (pageResult != null && pageResult.size() > 0) {
            for (Orders orders : pageResult){
                // 查询该订单的所有菜品
                Long ordersId = orders.getId();
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(ordersId);

                // 拷贝基础信息
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            }
        }
        return new PageResult(pageResult.getTotal(), list);
    }


    /**
     * 查询订单详细
     */
    @Override
    public OrderVO  details(Long id){
        // 1.根据id查询订单
        Orders orders = orderMapper.getById(id);

        // 2. 查询该订单对应的菜品明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());


        // 3.封装成Vo层返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 根据id删除订单
     * @param id
     */
    @Override
    public void userCancelById(Long id){
        // 得出用户id
        Long userId = BaseContext.getCurrentId();


        // 只能查询当前用户自己的订单
        Orders order = orderMapper.getByIdAndUserId(id, userId);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 只允许取消“待付款”或“待接单”的订单
        Integer status = order.getStatus();
        if (!Orders.PENDING_PAYMENT.equals(status)
                && !Orders.TO_BE_CONFIRMED.equals(status)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        // 组装要改的字段；Mapper 的 update 会只更新非空字段
        Orders updateOrder = Orders.builder()
                .id(id)
                .status(Orders.CANCELLED)
                .cancelReason("用户取消")
                .cancelTime(LocalDateTime.now())
                .build();


        // 当前项目是模拟支付：这里只能记录“模拟退款”状态，不会真的退微信款
        if (Orders.PAID.equals(order.getPayStatus())) {
            updateOrder.setPayStatus(Orders.REFUND);
        }
        orderMapper.update(updateOrder);


    }


    /**
     * 再来一单
     * @param id
     */
    @Override
    public void repetition(Long id){
        // 查询当前id
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 把订单对象转为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            return shoppingCart;
        }).collect(Collectors.toList());


        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }



    // ----- 管理端 -----
    /**
     *
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO){
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        // 部分订单状态，需要额外返回订单菜品信息，将Orders转化为OrderVO
        List<OrderVO> orderVOList = getOrderVOList(page);

        return new PageResult(page.getTotal(), orderVOList);
    }


    private List<OrderVO> getOrderVOList(Page<Orders> page) {
        // 需要返回订单菜品信息，自定义OrderVO响应结果
        List<OrderVO> orderVOList = new ArrayList<>();

        List<Orders> ordersList = page.getResult();
        if (!CollectionUtils.isEmpty(ordersList)) {
            for (Orders orders : ordersList) {
                // 将共同字段复制到OrderVO
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders, orderVO);
                String orderDishes = getOrderDishesStr(orders);

                // 将订单菜品信息封装到orderVO中，并添加到orderVOList
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }


    /**
     * 根据id搜索菜品信息字符串
     *
     * @param orders
     * @return
     */
    private String getOrderDishesStr(Orders orders){
        // 查询订单菜品详情信息（订单中的菜品和数量）
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将每一条订单菜品信息拼接为字符串（格式：宫保鸡丁*3；）
        List<String> orderDishList = orderDetailList.stream().map(x -> {
            String orderDish = x.getName() + "*" + x.getNumber() + ";";
            return orderDish;
        }).collect(Collectors.toList());

        // 将该订单对应的所有菜品信息拼接在一起
        return String.join("", orderDishList);
    }


}
