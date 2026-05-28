package com.concer.backend.Selenium.Dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddAreaDto {
    private String areaName;  // 座位名稱
    private String areaPrice; // 價格
    private String qty;
}



