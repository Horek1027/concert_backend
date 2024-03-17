package com.concer.backend.orders.Service;

import com.concer.backend.Request.FindUserByAccountRequst;
import com.concer.backend.Request.OrderAddRequest;
import com.concer.backend.Response.OrdersResponse;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.orders.Entity.Orders;

import java.util.List;

public interface OrderService {
    RestfulResponse<String> insert (List<OrderAddRequest>req);
    RestfulResponse<List<OrdersResponse>> getUserOrders( FindUserByAccountRequst account);
}
