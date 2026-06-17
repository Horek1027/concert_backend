package com.concer.backend.area.MyBatisPlus;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concer.backend.orders.Entity.Orders;
import com.concer.backend.orders.MyBatisPlus.MyBatisPlusOrdersEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MyBatisPlusAreaMapper  extends BaseMapper<MyBatisPlusAreaEntity> {
    int checkAndUpdateQty(@Param("orders") List<MyBatisPlusOrdersEntity> orders);

    int refundQty(@Param("orders") List<MyBatisPlusOrdersEntity> orders);

}
