package com.concer.backend.orders.Service;

import com.concer.backend.Request.OrderAddRequest;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.orders.Entity.Orders;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService{

    @Override
    public RestfulResponse<String> insert(OrderAddRequest req) {
        return null;
    }

    @Override
    public RestfulResponse<Iterable<Orders>> getAllEvents() {
        return null;
    }
}
