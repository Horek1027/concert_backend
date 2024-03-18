package com.concer.backend.orders.DAO;

import com.concer.backend.Request.OrderAddRequest;
import com.concer.backend.Response.OrdersResponse;
import com.concer.backend.orders.Entity.Orders;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository

public interface OrdersRepository  extends JpaRepository<Orders,Integer> {
    List<Orders> findByUserId(Integer userId);
    @Query("Select o From Orders o Where o.userId = ?1 And o.eventsId = ?2")
    List<Orders> findByUserIdAndEventsId(Integer userId, Integer eventsId);
    @Modifying
    @Query("Update Orders o Set o.orderStatus = 1 Where  o.userId= ?1 And o.eventsId= ?2")
    void updateStatus(Integer userId , Integer eventsId);


}
