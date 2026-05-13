package com.concer.backend.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventsAndAreaRequest {
    private EventsAddRequest eventAddData;
    private List<AreaAddRequest> areaAddData; // 将其定义为 List<AreaAddRequest>
}
