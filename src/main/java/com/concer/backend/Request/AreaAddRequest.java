package com.concer.backend.Request;


import com.concer.backend.events.Entity.Events;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AreaAddRequest {
    private String areaName;

    private Integer areaPrice;

    private Integer qty;
    private Events eventsId;
}
