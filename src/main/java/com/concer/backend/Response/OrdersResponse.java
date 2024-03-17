package com.concer.backend.Response;

import com.concer.backend.events.Entity.Events;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrdersResponse {

    private Integer orderId;
    private Integer userId;
    private Integer eventsId;

    private String orderArea;
    private Integer orderQty;
    private Integer orderPrice;
    private Date orderDate;
    private Integer odrderStatus;
    private String eventsName;
}
