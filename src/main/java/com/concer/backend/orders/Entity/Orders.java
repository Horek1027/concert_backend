package com.concer.backend.orders.Entity;

import com.concer.backend.events.Entity.Events;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name= "orders")
public class Orders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="order_id")
    private Integer orderId;

    @Column(name="users_id")
    private Integer userId;
    @Column(name = "events_id" )
    private Integer eventsId;
    @Column(name="order_area")
    private String orederArea;
    @Column(name="order_qty")
    private Integer orderQty;
    @Column(name="order_price")
    private Integer orderPrice;

    @Column(name="order_date")
    private Date orderDate;
    @Column(name="order_status")
    private Integer order_status;



}
