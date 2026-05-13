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
public class EventsRequest {
    private Integer userId;
    private String evnetsName;
    private String eventsDetails;
    private String eventsLocation;
    private String eventsOrganizer;
    private String eventDate;
    private Date shelfTime;
    private Date offSaleTime;
    private String image1;

    private List<Area> areas;
}
