package com.sky.controller.user;


import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/user/order")
@RestController("userOrderController")
@Api(tags = "用户端订单接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 用户订单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        log.info("用户下单: {}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }



    /**
     * 订单支付
     */
    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 用户查询历史订单
     * @param page,pageSize,status
     * @return
     */
    @GetMapping("/historyOrders")
    @ApiOperation("用户历史订单查询")
    public Result<PageResult> page(Integer page, Integer pageSize, Integer status){
        log.info("历史订单查询: page = {}, pageSize = {} ,status = {}",page,pageSize,status);
        PageResult pageResult = orderService.pageQueryUser(page, pageSize, status);
        return Result.success(pageResult);
    }

    /**
     * 查询退款订单（历史订单「退款」tab）
     * @param page,pageSize,status,payStatus
     * @return
     */
    @GetMapping("/queryOrdersCheckStatus")
    @ApiOperation("查询退款订单")
    public Result<PageResult> queryOrdersCheckStatus(Integer page, Integer pageSize, Integer status, Integer payStatus){
        log.info("退款订单查询: page = {}, pageSize = {}, status = {}, payStatus = {}", page, pageSize, status, payStatus);
        return Result.success(orderService.pageQueryUser(page, pageSize, status, payStatus));
    }


    /**
     * 查询订单详信息
     */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("查询订单详细信息")
    public Result<OrderVO> details(@PathVariable("id") Long id){
        log.info("根据id查询订单详细信息: {}",id);
        OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);
    }


    /**
     * 用户取消订单
     * @param id
     * @return
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("用户取消订单")
    public Result<String> cancel(@PathVariable Long id){
        orderService.userCancelById(id);
        return Result.success();
    }


    /**
     * 再来一单
     *
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result repetition(@PathVariable Long id){
        log.info("再来一单");
        orderService.repetition(id);
        return Result.success();
    }


    /**
     * 用户催单
     * @param id
     * @return
     */
    @GetMapping("/reminder/{id}")
    @ApiOperation("用户催单")
    public Result reminder(@PathVariable("id") Long id){
        log.info("用户催单: {}", id);
        orderService.reminder(id);
        return Result.success();
    }


    /**
     * 用户申请退款（已接单/派送中/已完成的已支付订单）
     * @param id
     * @return
     */
    @PostMapping("/refund/{id}")
    @ApiOperation("用户申请退款")
    public Result refund(@PathVariable("id") Long id){
        log.info("用户申请退款: {}", id);
        orderService.refundApply(id);
        return Result.success();
    }

}
