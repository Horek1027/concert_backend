package com.concer.backend.kafka.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReserveRequest {
    private Integer eventsId;
    private String orderArea;
    private String orderId;
    private int qty;
    private String action;
    // 新增：這張訂單總共有幾筆拆單
    private Integer totalSegments;
}