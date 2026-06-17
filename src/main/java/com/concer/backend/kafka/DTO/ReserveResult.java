package com.concer.backend.kafka.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReserveResult {
    private String orderId;
    private boolean success;
    private String message;
}
