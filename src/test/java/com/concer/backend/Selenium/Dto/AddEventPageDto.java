package com.concer.backend.Selenium.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddEventPageDto {

    private String fileUploadInput;
    private String eventName;
    private String host;
    private String eventDate;
    private String eventsLocation;
    private String shelfTimeDate;
    private String offTimeDate;
    private String eventDetail;



}
