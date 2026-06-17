package com.concer.backend.Selenium.Dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class AddAreaDto {
    private String areaName;  // 座位名稱
    private String areaPrice; // 價格
    private String qty;

    public static List<AddAreaDto> createValidDto() {
        List<AddAreaDto> list = new ArrayList<>();
        list.add(AddAreaDto.builder().areaName("搖滾 A 區").areaPrice("4800").qty("20").build());
        list.add(AddAreaDto.builder().areaName("看台 B 區").areaPrice("3200").qty("50").build());
        list.add(AddAreaDto.builder().areaName("體驗 C 區").areaPrice("800").qty("50").build());
        return  list;
    }
}



