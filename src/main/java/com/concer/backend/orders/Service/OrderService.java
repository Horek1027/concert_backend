package com.concer.backend.orders.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.concer.backend.Request.FindUserByAccountRequst;
import com.concer.backend.Request.OrderAddRequest;
import com.concer.backend.Request.OrderCancelRequest;
import com.concer.backend.Response.OrderMergeData;
import com.concer.backend.Response.OrdersResponse;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.kafka.Event.OrderCreatedEvent;
import com.concer.backend.orders.Entity.Orders;
import com.concer.backend.orders.MyBatisPlus.MyBatisPlusOrdersEntity;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

public interface OrderService extends IService<MyBatisPlusOrdersEntity> {
    RestfulResponse<String> insert (List<OrderAddRequest>req);
    void insertFromKafka( OrderCreatedEvent event) throws JsonProcessingException;

    RestfulResponse<List<OrderMergeData>> getUserOrders(FindUserByAccountRequst account);
    RestfulResponse<String> cancelOrders(OrderCancelRequest req);
}
