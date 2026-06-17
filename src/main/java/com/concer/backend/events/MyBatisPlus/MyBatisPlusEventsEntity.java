package com.concer.backend.events.MyBatisPlus;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.concer.backend.area.Entity.Area;
import com.concer.backend.area.MyBatisPlus.MyBatisPlusAreaEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("events")
public class MyBatisPlusEventsEntity {

    @TableId(value = "events_id", type = IdType.AUTO) // 2. 替換原來的 @Id 與主鍵生成策略
    private Integer eventsId;
    // 3. 因為開啟了底線轉駝峰（user_id -> userId），MyBatis-Plus 會自動對應，一般欄位不需加註解
    @TableField("user_id")
    private Integer userId;
    private String eventsName;
    private String eventsDetails;
    private String eventsLocation;
    private String eventsOrganizer;
    private String eventDate;
    private Date shelfTime; // 自動對應 db 欄位：shelf_time
    @TableField("offsale_time")
    private Date offSaleTime;
    private String image1;

    /**
     * 4. 關聯處理：
     * MyBatis-Plus 不支援 @OneToMany 級聯操作。
     * 如果你需要保留這個 List 來存放關聯資料，必須加上 exist = false，
     * 否則 MyBatis-Plus 在執行單表 INSERT/UPDATE 時會因為找不到 "area" 欄位而崩潰。
     */
    @TableField(exist = false)
    private List<MyBatisPlusAreaEntity> area;

    public void add(MyBatisPlusAreaEntity tempArea){
        if(area == null){
            area = new ArrayList<>();
        }
        area.add(tempArea);
    }
}
//取代 @OneToMany 的後續做法
// public void saveEventWithAreas(Events events, List<Area> areas) {
//    // 1. 先存 events 拿到自增的 eventsId
//    eventsMapper.insert(events);
//
//    // 2. 跑迴圈把 eventsId 塞給每個 area，並手動存入 area 表
//    for(Area area : areas) {
//        area.setEventsId(events.getEventsId());
//        areaMapper.insert(area);
//    }
//}