package com.concer.backend.kafka.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReserveResult {
    private String orderId;
    private Integer eventsId;
    private String orderArea;
    private Integer qty;
    private boolean success;
    private String message;

    // 新增
    private Integer totalSegments;

    // 聚合後用
    private List<ReserveResult> details;
}
