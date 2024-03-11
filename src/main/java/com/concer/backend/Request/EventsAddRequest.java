package com.concer.backend.Request;

import com.concer.backend.area.Entity.Area;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventsAddRequest {
    private String account;
    private String eventsName;

    private String eventsDetails;

    private String eventsLocation;
    private String eventsOrganizer;

    private String image1;

    private String eventDate;

    private String shelfTime;

    private String offSaleTime;


}
