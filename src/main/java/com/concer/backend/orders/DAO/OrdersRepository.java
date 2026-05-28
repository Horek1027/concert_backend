package com.concer.backend.orders.DAO;

import com.concer.backend.Request.OrderAddRequest;
import com.concer.backend.Response.OrdersResponse;
import com.concer.backend.orders.Entity.Orders;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository

public interface OrdersRepository  extends JpaRepository<Orders,Integer> {
    List<Orders> findByUserId(Integer userId);
    @Query("Select o From Orders o Where o.orderId In ?1")
    List<Orders> findByUserIdAndEventsId(List<Integer> orderIds);
    @Modifying
    @Query("Update Orders o Set o.orderStatus = 1 Where o.orderId In ?1")
    int updateStatusByOrderIds(List<Integer> orderIds);


}
