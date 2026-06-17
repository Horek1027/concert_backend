package com.concer.backend.orders.MyBatisPlus;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concer.backend.orders.Entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
@Mapper
public interface MyBatisPlusOrdersMapper extends BaseMapper<MyBatisPlusOrdersEntity> {
    int insertBatch(@Param("list") List<MyBatisPlusOrdersEntity> orderList);

}
