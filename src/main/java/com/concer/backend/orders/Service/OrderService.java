package com.concer.backend.orders.Service;

import com.concer.backend.Request.OrderAddRequest;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.orders.Entity.Orders;

public interface OrderService {
    RestfulResponse<String> insert (OrderAddRequest req);

    RestfulResponse<Iterable<Orders>> getAllEvents ();
}
