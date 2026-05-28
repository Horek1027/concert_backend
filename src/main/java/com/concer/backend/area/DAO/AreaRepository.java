package com.concer.backend.area.DAO;

import com.concer.backend.area.Entity.Area;
import com.concer.backend.events.Entity.Events;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AreaRepository extends JpaRepository<Area, Integer> {
    // 使用 @Query 結合 List 進行批量查詢，所有請求都在同一個活動內
    @Query("select a from Area a where a.eventsId.eventsId = :eventsId and a.areaName in :areaNames")
    List<Area> findAreas(@Param("eventsId") Integer eventsId,
                         @Param("areaNames") List<String> areaNames);


    //使用雙條件找尋資料時就要自訂JPQL查詢
    @Query("SELECT a From Area a where a.eventsId= ?1 And a.areaName=?2")
    Area findByEventsIdAndAreaName(Events eventsId, String areaName);


 //  清除 改由方法的JDBC執行 `?1` 是傳進 Entity 的狀況，直接傳 ID，JPA 在處理 Batch 時會更流暢
 //    @Modifying
//    @Query("Update Area a Set a.qty = a.qty + ?3 Where a.eventsId.eventsId = ?1 And a.areaName = ?2")
//    void refundQty(Integer eventsId, String areaName, Integer qty);

}
