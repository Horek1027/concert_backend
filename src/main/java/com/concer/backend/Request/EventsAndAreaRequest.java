package com.concer.backend.Request;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventsAndAreaRequest {
    @NotNull(message = "eventAddData物件不可為空")
    @Valid // 加這行才會深入檢查 EventsAddRequest 裡面的欄位！
    private EventsAddRequest eventAddData;
    @NotNull(message = "areaAddData物件不可為空")
    @Valid
    private List<AreaAddRequest> areaAddData;
}
