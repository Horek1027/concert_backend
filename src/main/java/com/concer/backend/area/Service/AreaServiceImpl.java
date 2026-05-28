package com.concer.backend.area.Service;
import com.concer.backend.Request.OrderAddRequest;
import com.concer.backend.area.DAO.AreaRepository;
import com.concer.backend.area.Entity.Area;
import com.concer.backend.events.Entity.Events;
import com.concer.backend.orders.Entity.Orders;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AreaServiceImpl implements AreaService {
    @Autowired
    private AreaRepository areaRepository;
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void insert(List<Area> areas) {
        for (Area a : areas) {
            areaRepository.save(a);
        }
    }

    @Override
    public boolean checkQty(List<OrderAddRequest> req) {
        if (req == null || req.isEmpty()) return true;

        // 1. 整理出該活動下，所有需要檢查的區域名稱清單
//        Integer eventsId = ; // 假設同一個活動
        List<String> areaNames = req.stream()
                .map(OrderAddRequest::getOrderArea)
                .distinct()
                .collect(Collectors.toList());

        log.info("獲得的eventsId:" + req.get(0).getEventsId());
        log.info("獲得的eventsId:" + req.get(0).getEventsId());


        // 2.一次性從資料庫查出所有相關區域的庫存（只查詢 1 次！）
        List<Area> areas = areaRepository.findAreas(req.get(0).getEventsId(), areaNames);

        // 3. 轉成 Map 方便快速查找 (AreaName -> Area)
        Map<String, Area> areaMap = areas.stream()
                .collect(Collectors.toMap(Area::getAreaName, a -> a));

        // 4. 在記憶體中進行比對，不再碰資料庫
        for (OrderAddRequest data : req) {
            Area area = areaMap.get(data.getOrderArea());
            if (area == null || area.getQty() < Integer.parseInt(data.getOrderQty())) {
                return false;
            }
        }
        return true;
    }
//    public Optional<Boolean> checkQty(List<OrderAddRequest> req) {
//        for(OrderAddRequest data:req){
//            Events events = new Events();
//            events.setEventsId(data.getEventsId());
//            Area area = areaRepository.findByEventsIdAndAreaName(events, data.getOrderArea());
//            //使用save方法進行跟新， 會把全部欄位都更新過(效率較不好)
//            if(area.getQty() < Integer.parseInt(data.getOrderQty())){
//                return Optional.of(false);
//            }
//        }
//        return Optional.of(true);
//    }


    @Override
    @Transactional
    public void updateQty(List<Orders> orders) {
        Session session = entityManager.unwrap(Session.class);
        session.doWork(connection -> {
            //這邊是原生SQL
            String sql = "UPDATE Area SET qty = qty - ? WHERE events_id = ? AND area_name = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                for (Orders order : orders) {
                    ps.setInt(1, order.getOrderQty());
                    ps.setInt(2, order.getEventsId());
                    ps.setString(3, order.getOrderArea());
                    ps.addBatch();
                }
                ps.executeBatch(); // 一次性更新所有庫存
            }
        });
    }

    @Override
    @Transactional
    public void refundQty(List<Orders> orders) {
        int batchSize = 50;

        // 這裡的 Session 類別，現在已經正確對應到 org.hibernate.Session 了
        Session session = entityManager.unwrap(Session.class);

        session.doWork(connection -> {
            String sql = "UPDATE Area SET qty = qty + ? WHERE events_id = ? AND area_name = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                for (int i = 0; i < orders.size(); i++) {
                    Orders data = orders.get(i);

                    ps.setInt(1, data.getOrderQty());
                    ps.setInt(2, data.getEventsId());
                    ps.setString(3, data.getOrderArea());

                    ps.addBatch(); //把每筆資料都暫存在記憶體中
                    //不使用.executeUpdate();即使修改properties 還是會修改table

                    if ((i + 1) % batchSize == 0) {
                        ps.executeBatch(); //這一行執行完，JDBC 就會自動清空這 50 筆參數的記憶體了
                    }
                }
                ps.executeBatch(); //少於50筆資料此行送出
            }
        });
    }


}
