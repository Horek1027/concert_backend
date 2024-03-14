package com.concer.backend.orders.DAO;

import com.concer.backend.orders.Entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface OrdersRepository  extends JpaRepository<Orders,Integer> {
}
