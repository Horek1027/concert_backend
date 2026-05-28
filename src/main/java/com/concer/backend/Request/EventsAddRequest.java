package com.concer.backend.Request;

import com.concer.backend.area.Entity.Area;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventsAddRequest {
    @NotBlank(message = "account不可為空")
    private String account;
    private String eventsName;
    private String eventsDetails;
    private String eventsLocation;
    private String eventsOrganizer;
    private String image1;
    @NotNull(message = "eventDate不可為空")
    private String eventDate;
    private String shelfTime;
    private String offSaleTime;
}
