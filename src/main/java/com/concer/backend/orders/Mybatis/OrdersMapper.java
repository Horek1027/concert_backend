package com.concer.backend.orders.Mybatis;

import com.concer.backend.orders.Entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
@Mapper
public interface OrdersMapper {
    int insertBatch(@Param("list") List<Orders> orderList);
}
