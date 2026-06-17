package com.concer.backend.area.MyBatisPlus;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("area")
public class MyBatisPlusAreaEntity {
    @TableId(value = "area_id", type = IdType.AUTO)
    private Integer areaId;
    private Integer eventsId;
    private String areaName;
    private Integer areaPrice;
    private Integer qty;
}
