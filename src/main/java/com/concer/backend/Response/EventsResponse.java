package com.concer.backend.Response;

import com.concer.backend.area.Entity.Area;

import java.util.Date;
import java.util.List;

public class EventsResponse {

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
