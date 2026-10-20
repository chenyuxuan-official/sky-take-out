package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public TurnoverReportVO getTurnover(LocalDate begin, LocalDate end){
        List<LocalDate> dateList = new ArrayList<>();

        for (LocalDate d = begin; !d.isAfter(end); d = d.plusDays(1)) {
            dateList.add(d);
        }

        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList){
            LocalDateTime beginTime = date.atStartOfDay();
            LocalDateTime endTime = date.plusDays(1).atStartOfDay();
            Map map = new HashMap();
            map.put("status", Orders.COMPLETED);
            map.put("begin",beginTime);
            map.put("end",endTime);

            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }


        // 数据封装
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }


    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();

        for (LocalDate d = begin; !d.isAfter(end); d = d.plusDays(1)) {
            dateList.add(d);
        }
        List<Integer> newUserList = new ArrayList<>(); // 新增用户
        List<Integer> totalUserList = new ArrayList<>(); // 总用户数

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = date.atStartOfDay();
            LocalDateTime endTime = date.plusDays(1).atStartOfDay();

            Integer newUser = getUserCount(beginTime, endTime);
            Integer totalUser = getUserCount(null, endTime);

            newUserList.add(newUser);
            totalUserList.add(totalUser);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .build();
    }


    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        for (LocalDate d = begin; !d.isAfter(end); d = d.plusDays(1)) {
            dateList.add(d);
        }

        List<Integer> orderCountList = new ArrayList<>(); // 每日订单数
        List<Integer> validOrderCountList = new ArrayList<>(); // 每日有效订单数
        Integer totalOrderCount = 0; // 订单总数
        Integer validOrderCount = 0; // 有效订单数

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = date.atStartOfDay();
            LocalDateTime endTime = date.plusDays(1).atStartOfDay();

            Integer orderCount = getOrderCount(beginTime, endTime, null);
            Integer dailyValidOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(dailyValidOrderCount);
            totalOrderCount += orderCount;
            validOrderCount += dailyValidOrderCount;
        }

        Double orderCompletionRate = totalOrderCount == 0 ? 0.0 : validOrderCount * 1.0 / totalOrderCount;

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }



    /**
     * 根据时间区间统计用户数量
     * @param beginTime
     * @param endTime
     * @return
     */
    private Integer getUserCount(LocalDateTime beginTime, LocalDateTime endTime) {
        Map map = new HashMap();
        map.put("begin",beginTime);
        map.put("end", endTime);
        return userMapper.countByMap(map);
    }

    /**
     * 根据时间区间统计订单数量
     * @param beginTime
     * @param endTime
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map<String, Object> map = new HashMap<>();
        map.put("begin", beginTime);
        map.put("end", endTime);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }


    /**
     * 查询指定时间区间内的销量派梦Top10
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin,LocalDate end){
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end,LocalTime.MAX);
        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getSalesTop10(beginTime, endTime);

        String nameList = StringUtils.join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList()),",");
        String numberList = StringUtils.join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList()),",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }
}
