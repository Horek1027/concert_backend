package com.concer.backend.orders.DAO;

import com.concer.backend.Request.OrderAddRequest;
import com.concer.backend.Response.OrdersResponse;
import com.concer.backend.orders.Entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface OrdersRepository  extends JpaRepository<Orders,Integer> {
    List<Orders> findByUserId(Integer userId);

}
