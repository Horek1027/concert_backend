package com.concer.backend.Request;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderCancelRequest {
    private  Integer userId;
    private Integer eventsId;
}
