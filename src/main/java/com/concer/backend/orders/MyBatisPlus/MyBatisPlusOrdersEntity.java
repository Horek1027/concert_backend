package com.concer.backend.orders.MyBatisPlus;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("orders")
public class MyBatisPlusOrdersEntity {
    @TableId(value = "order_id", type = IdType.AUTO)
    private Integer orderId;
    private Integer userId;
    private Integer eventsId;
    private String orderArea;
    private Integer orderQty;
    private Integer orderPrice;
    private Date orderDate;
    private Integer orderStatus;
}
