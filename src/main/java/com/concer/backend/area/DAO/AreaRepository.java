package com.concer.backend.area.DAO;

import com.concer.backend.area.Entity.Area;
import com.concer.backend.events.Entity.Events;
import com.concer.backend.orders.Entity.Orders;
import org.hibernate.query.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AreaRepository  extends JpaRepository<Area,Integer> {
    //使用雙條件找尋資料時就要自訂JPQL查詢
    @Query("SELECT a From Area a where a.eventsId= ?1 And a.areaName=?2")
    Area findByEventsIdAndAreaName(Events eventsId, String areaName);



}
