package com.concer.backend.orders.Control;


import com.concer.backend.Request.FindUserByAccountRequst;
import com.concer.backend.Request.OrderAddRequest;
import com.concer.backend.Request.OrderCancelRequest;
import com.concer.backend.Response.OrderMergeData;
import com.concer.backend.Response.OrdersResponse;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.orders.Service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrdersControl {
    @Autowired
    private OrderService orderService;

    @PostMapping("/add")
    public RestfulResponse<String> add(@RequestBody @Valid List<OrderAddRequest> req){
        return orderService.insert(req);
    }

    @PostMapping("/")
    public RestfulResponse<List<OrderMergeData>> getUserOrders(@RequestBody FindUserByAccountRequst req){
        return orderService.getUserOrders(req);
    }
    @PutMapping("/cancel")
    public  RestfulResponse<String> updateStatus(@RequestBody OrderCancelRequest req){
        return orderService.cancelOrders(req);
    }
}
