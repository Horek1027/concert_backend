package com.concer.backend.area.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concer.backend.Request.OrderAddRequest;
import com.concer.backend.area.DAO.AreaRepository;
import com.concer.backend.area.Entity.Area;
import com.concer.backend.area.MyBatisPlus.MyBatisPlusAreaEntity;
import com.concer.backend.area.MyBatisPlus.MyBatisPlusAreaMapper;
import com.concer.backend.orders.Entity.Orders;
import com.concer.backend.orders.MyBatisPlus.MyBatisPlusOrdersEntity;
import com.concer.backend.orders.MyBatisPlus.MyBatisPlusOrdersMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AreaServiceImpl
        extends ServiceImpl<MyBatisPlusAreaMapper, MyBatisPlusAreaEntity>
        implements AreaService {

    private final SqlSessionTemplate sqlSessionTemplate;

//    private final AreaMapper areaMapper;
//    private final AreaRepository areaRepository;
    private final MyBatisPlusAreaMapper myBatisPlusAreaMapper;
    @PersistenceContext
    private  EntityManager entityManager;

    @Override
    public void insert(List<MyBatisPlusAreaEntity> areas) {
//        for (Area a : areas) {
//            areaRepository.save(a);
//        }
        this.saveBatch(areas);
    }


    @Override // 目前不使用
    public boolean checkQty(List<OrderAddRequest> req) {

        if (req == null || req.isEmpty()) return true;

        // 1. 整理出該活動下，所有需要檢查的區域名稱清單
//        Integer eventsId = ; // 假設同一個活動
        List<String> areaNames = req.stream()
                .map(OrderAddRequest::getOrderArea)
                .distinct()
                .collect(Collectors.toList());

        log.info("獲得的eventsId:" + req.get(0).getEventsId());

        // 2.一次性從資料庫查出所有相關區域的庫存（只查詢 1 次！）
//        List<Area> areas = areaRepository.findAreas(req.get(0).getEventsId(), areaNames);

        List<MyBatisPlusAreaEntity> areas =myBatisPlusAreaMapper.selectList(
                new LambdaQueryWrapper<MyBatisPlusAreaEntity>()
                        .eq(MyBatisPlusAreaEntity::getEventsId ,req.get(0).getEventsId())
                        .in(MyBatisPlusAreaEntity::getAreaName,areaNames)
        );
        // 3. 轉成 Map 方便快速查找 (AreaName -> Area)
        Map<String, MyBatisPlusAreaEntity> areaMap = areas.stream()
                .collect(Collectors.toMap(MyBatisPlusAreaEntity::getAreaName, a -> a));

        // 4. 在記憶體中進行比對，不再碰資料庫
        for (OrderAddRequest data : req) {
            MyBatisPlusAreaEntity area = areaMap.get(data.getOrderArea());
            if (area == null || area.getQty() < Integer.parseInt(data.getOrderQty())) {
                return false;
            }
        }
        return true;
    }


    @Override
    @Transactional
    public boolean checkAndUpdateQty(List<MyBatisPlusOrdersEntity> orders) {

//        Session session = entityManager.unwrap(Session.class);
//        session.doWork(connection -> {
//            //這邊是原生SQL
//            String sql = "UPDATE Area SET qty = qty - ? WHERE events_id = ? AND area_name = ?";
//            try (PreparedStatement ps = connection.prepareStatement(sql)) {
//                for (Orders order : orders) {
//                    ps.setInt(1, order.getOrderQty());
//                    ps.setInt(2, order.getEventsId());
//                    ps.setString(3, order.getOrderArea());
//                    ps.addBatch();
//                }
//                ps.executeBatch(); // 一次性更新所有庫存
//            }
//        });

//        log.info( "定位區收到的資訊: " + orders.toString());
//        int updatedCount = myBatisPlusAreaMapper.checkAndUpdateQty(orders);
//        if (updatedCount < orders.size()) {
//            throw new RuntimeException("票券數量不足，全部回滾");
//        }

        //配合consumeResult 的批次寫法
        if (orders == null || orders.isEmpty()) return true;
        log.info("定位區收到的原始資訊筆數: " + orders.size());

        // 1. 💡 在記憶體內將「相同活動 + 相同區域」的扣減數量進行加總 (Merge)
        Map<String, MyBatisPlusOrdersEntity> mergedMap = new HashMap<>();

        for (MyBatisPlusOrdersEntity order : orders) {
            // 用活動 ID 和區域組成 unique key
            String key = order.getEventsId() + "_" + order.getOrderArea();

            if (mergedMap.containsKey(key)) {
                MyBatisPlusOrdersEntity existing = mergedMap.get(key);
                // 票數累加
                existing.setOrderQty(existing.getOrderQty() + order.getOrderQty());
            } else {
                // 複製一份物件，避免污染原本的 orders 實體
                MyBatisPlusOrdersEntity clone = new MyBatisPlusOrdersEntity();
                clone.setEventsId(order.getEventsId());
                clone.setOrderArea(order.getOrderArea());
                clone.setOrderQty(order.getOrderQty());
                mergedMap.put(key, clone);
            }
        }

        // 2. 💡 關鍵：對合併後的結果進行「嚴格排序」(Sort)
        // 依據 eventsId 排序，若相同則依據 orderArea 排序。確保鎖表順序一致，徹底杜絕死鎖！
        List<MyBatisPlusOrdersEntity> optimizedOrders = new ArrayList<>(mergedMap.values());
        optimizedOrders.sort(Comparator
                .comparing(MyBatisPlusOrdersEntity::getEventsId)
                .thenComparing(MyBatisPlusOrdersEntity::getOrderArea)
        );

        log.info("優化（合併與排序）後的 SQL 傳入筆數: " + optimizedOrders.size());

        //
        int updatedCount = myBatisPlusAreaMapper.checkAndUpdateQty(optimizedOrders);

        // ⚠️ 注意：因為前面合併了同區域的訂單，updatedCount 代表的是「成功更新的資料列（Row）數量」
        // 它會等於 optimizedOrders.size()，而不一定等於原始的 orders.size()！
        if (updatedCount < optimizedOrders.size()) {
            throw new RuntimeException("票券數量不足或區域不匹配，全部回滾");
        }
        return true;
    }

    @Override
    @Transactional
    public boolean refundQty(List<MyBatisPlusOrdersEntity> orders) {
//        int batchSize = 50;
//
//        // 這裡的 Session 類別，現在已經正確對應到 org.hibernate.Session 了
//        Session session = entityManager.unwrap(Session.class);
//
//        session.doWork(connection -> {
//            String sql = "UPDATE Area SET qty = qty + ? WHERE events_id = ? AND area_name = ?";
//            try (PreparedStatement ps = connection.prepareStatement(sql)) {
//                for (int i = 0; i < orders.size(); i++) {
//                    Orders data = orders.get(i);
//
//                    ps.setInt(1, data.getOrderQty());
//                    ps.setInt(2, data.getEventsId());
//                    ps.setString(3, data.getOrderArea());
//
//                    ps.addBatch(); //把每筆資料都暫存在記憶體中
//                    //不使用.executeUpdate();即使修改properties 還是會修改table
//
//                    if ((i + 1) % batchSize == 0) {
//                        ps.executeBatch(); //這一行執行完，JDBC 就會自動清空這 50 筆參數的記憶體了
//                    }
//                }
//                ps.executeBatch(); //少於50筆資料此行送出
//            }
//        });


        // MyBatis 會自動走你在 XML 寫的 <foreach> SQL

        int result = myBatisPlusAreaMapper.refundQty(orders);
        log.info("orders.size() : " +orders.size());
        log.info("result : " + result);
        return result == orders.size();
    }


}
