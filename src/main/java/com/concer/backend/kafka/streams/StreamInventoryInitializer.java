package com.concer.backend.kafka.streams;

import com.concer.backend.area.MyBatisPlus.MyBatisPlusAreaEntity;
import com.concer.backend.area.Service.AreaService;
import com.concer.backend.kafka.DTO.ReserveRequest;
import com.concer.backend.kafka.DTO.UserSyncRequest;
import com.concer.backend.kafka.KafkaTopics;
import com.concer.backend.users.MyBatisPlus.MyBatisPlusUsersEntity;
import com.concer.backend.users.Service.UsersService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka Streams RocksDB 庫存預熱初始化器
 * <p>
 * 觸發時機：Spring Boot 啟動完成後自動執行（ApplicationRunner）。
 * 運作方式：
 *   1. 從 SQL 資料庫 area 表讀取所有區域的現存庫存（使用既有 AreaService.list()）。
 *   2. 依照 eventsId_areaName 作為 Kafka Key，發送初始化指令至 INVENTORY_INIT 主題。
 *   3. ReservationStreamProcessor 監聽此主題並將庫存寫入本地 RocksDB。
 * </p>
 * <p>
 * 若日後有管理員 API 需要手動重新載入庫存（例如演唱會改期），
 * 可直接呼叫 initializeRocksDB() 方法即可。
 * </p>
 */
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class StreamInventoryInitializer implements ApplicationRunner {
//
//    private final AreaService areaService;
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//    @Override
//    public void run(ApplicationArguments args) {
//        initializeRocksDB();
//    }
//
//    public void initializeRocksDB() {
//        log.info("【RocksDB 預熱】開始從 SQLDB 同步庫存至 Kafka Streams 本地狀態庫...");
//        try {
//            List<MyBatisPlusAreaEntity> areas = areaService.list();
//
//            if (areas == null || areas.isEmpty()) {
//                log.warn("【RocksDB 預熱】area 表無資料，跳過初始化。");
//                return;
//            }
//
//            for (MyBatisPlusAreaEntity area : areas) {
//                String kafkaKey = area.getEventsId() + "_" + area.getAreaName();
//
//                ReservationStreamProcessor.StreamRequest initReq =
//                        new ReservationStreamProcessor.StreamRequest();
//                initReq.orderId = "INIT";
//                initReq.qty = area.getQty();
//                initReq.action = "INIT";
//
//                // 直接發送至 RESERVE_REQUEST，由 KStreams 的 transformValues 攔截並寫入庫存
//                kafkaTemplate.send(KafkaTopics.RESERVE_REQUEST, kafkaKey, initReq);
//                log.debug("【RocksDB 預熱】已發送 Key: {}, Qty: {}", kafkaKey, area.getQty());
//            }
//            log.info("【RocksDB 預熱】完成！共初始化 {} 個區域庫存。", areas.size());
//        } catch (Exception e) {
//            log.error("【RocksDB 預熱】初始化失敗！請確認資料庫與 Kafka 連線狀態。", e);
//        }
//    }
//}

//放棄使用自訂factory 的版本
@Component
@Slf4j
@RequiredArgsConstructor
public class StreamInventoryInitializer implements ApplicationRunner {

    private final AreaService areaService;
    private final UsersService userService; // 💡 注入你的 UserService
    // 💡 泛型直接指定為我們要發送的物件類型 ReserveRequest
    private final KafkaTemplate<String, ReserveRequest> kafkaTemplate;
    private final KafkaTemplate<String, UserSyncRequest> userKafkaTemplate;

    @Override
    public void run(ApplicationArguments args) {
        initializeRocksDB();
        initializeUserRocksDB(); // 💡 同時預熱第二個表
    }

    public void initializeUserRocksDB() {
        log.info("【RocksDB 預熱】開始同步使用者資料...");
        List<MyBatisPlusUsersEntity> users = userService.list();

        for (MyBatisPlusUsersEntity user : users) {
            String kafkaKey = String.valueOf(user.getUserId()); // Key 用 userId

            UserSyncRequest initReq = new UserSyncRequest("INIT", user.getNickname(), user.getStatus());

            userKafkaTemplate.send(KafkaTopics.USER_SYNC, kafkaKey, initReq);
        }
        log.info("【RocksDB 預熱】使用者資料同步完成！");
    }

    public void initializeRocksDB() {
        log.info("【RocksDB 預熱】開始同步庫存...");
        //MyBatisPlus 的語法 這一行就把 table 的資料全部抓出
        List<MyBatisPlusAreaEntity> areas = areaService.list();

        //為每個座位 用 活動ID + _+ 區域名稱 做uniKey
        for (MyBatisPlusAreaEntity area : areas) {
            String kafkaKey = area.getEventsId() + "_" + area.getAreaName();

            // 💡 直接 new 出物件
            ReserveRequest initReq = new ReserveRequest("INIT", area.getQty(), "INIT");

            // 💡 直接丟入物件！Spring 會根據 properties 自動轉成標準 JSON 寫入 Kafka
            kafkaTemplate.send(KafkaTopics.RESERVE_REQUEST, kafkaKey, initReq);
        }
        log.info("【RocksDB 預熱】完成！");
    }
}
