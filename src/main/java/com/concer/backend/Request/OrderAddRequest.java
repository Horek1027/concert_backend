package com.concer.backend.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderAddRequest {
    String account;
    Integer eventsId;
    String orderArea;
    Integer orderQty;
    Integer orderPrice;

}
