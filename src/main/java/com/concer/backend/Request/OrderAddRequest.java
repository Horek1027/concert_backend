package com.concer.backend.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderAddRequest {

    private String account;

    private Integer eventsId;

    private String orderArea;

    private String orderQty;

    private Integer orderPrice;


}
