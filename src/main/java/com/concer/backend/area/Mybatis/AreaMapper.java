package com.concer.backend.area.Mybatis;


import com.concer.backend.orders.Entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
@Mapper
public interface AreaMapper {
    int checkAndUpdateQty(@Param("orders") List<Orders> orders);
}
