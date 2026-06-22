package com.concer.backend.kafka;

import com.concer.backend.area.Service.AreaService;
import com.concer.backend.kafka.DTO.ReserveResult;
import com.concer.backend.orders.Entity.OrderStatus;
import com.concer.backend.orders.MyBatisPlus.MyBatisPlusOrdersEntity;
import com.concer.backend.orders.Service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Kafka Streams 搶票結果接收器
 * <p>
 * 負責消費 RESERVE_RESULT 主題中由 ReservationStreamProcessor 輸出的扣減結果，
 * 並異步更新資料庫的訂單狀態（SUCCESS 或 FAILED），以及同步扣減 area 表的 qty。
 * </p>
 */
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class OrderResultConsumer {
//
//    private final OrderService orderService;
//    private final AreaService areaService;
//    private final ObjectMapper objectMapper = new ObjectMapper();

//    @KafkaListener(
//            topics = KafkaTopics.RESERVE_RESULT,
//            groupId = "order-result-group",
//            //指定使用我們自定義的工廠，開啟多執行緒並強制使用 String 接收
//            containerFactory = "stringKafkaListenerContainerFactory"
//    )
//    @Transactional(rollbackFor = Exception.class)
//    public void consumeResult(String message) {
//        try {
//            ResultPayload result = objectMapper.readValue(message, ResultPayload.class);
//            log.info("【搶票結果】明細單號: {}, 成功: {}, 原因: {}",
//                    result.orderId, result.success, result.message);
//
//            // 👈 修正點 1：防護 KStreams 拋出的 unknown 訂單號，避免 NumberFormatException 造成無限重試
//            if ("unknown".equals(result.orderId)) {
//                log.error("【搶票結果處理中斷】收到未知訂單號 (unknown)，略過處理。錯誤原因: {}", result.message);
//                return; // 直接 return 結束消費，不要拋出 Exception
//            }
//
//            Integer orderId;
//            try {
//                orderId = Integer.valueOf(result.orderId);
//            } catch (NumberFormatException e) {
//                log.error("【搶票結果處理中斷】訂單號格式錯誤: {}，略過處理。", result.orderId);
//                return;
//            }
//
//            OrderStatus finalStatus = result.success ? OrderStatus.SUCCESS : OrderStatus.FAILED;
//
//            // 1. 更新資料庫訂單狀態為最終狀態（SUCCESS or FAILED）
//            orderService.update(
//                    null,
//                    new LambdaUpdateWrapper<MyBatisPlusOrdersEntity>()
//                            .set(MyBatisPlusOrdersEntity::getOrderStatus, finalStatus.getCode())
//                            .eq(MyBatisPlusOrdersEntity::getOrderId, orderId)
//            );
//
//            log.info("【訂單狀態更新】orderId: {} → {}", orderId, finalStatus.getDescription());
//
//            // 2. 若搶票成功，同步扣減 area 表的 qty（確保 SQLDB 與 RocksDB 最終一致）
//            if (result.success) {
//                MyBatisPlusOrdersEntity order = orderService.getById(orderId);
//                if (order != null) {
//                    boolean synced = areaService.checkAndUpdateQty(
//                            java.util.Collections.singletonList(order)
//                    );
//                    if (synced) {
//                        log.info("【SQLDB 庫存同步】eventsId: {}, area: {}, qty: {} 扣減成功",
//                                order.getEventsId(), order.getOrderArea(), order.getOrderQty());
//                    } else {
//                        log.warn("【SQLDB 庫存同步】eventsId: {}, area: {} 扣減失敗（SQLDB 庫存可能不足）",
//                                order.getEventsId(), order.getOrderArea());
//                    }
//                }
//            }
//
//        } catch (JsonProcessingException e) {
//            // 👈 ⭐ 關鍵修正點：單獨捕捉 Jackson 解析異常（格式壞掉的毒藥訊息）
//            log.error("【嚴重警告】搶票結果 JSON 格式畸形，無法解析，系統已自動丟棄此訊息！原始訊息: {}", message, e);
//            // 這裡直接 return，不拋出異常！這樣 Kafka 就會認為這筆消費成功，繼續前進到下一個 Offset。
//
//        } catch (Exception e) {
//            // 👈 這裡依然保留：如果是資料庫掛掉、連線逾時等「系統級異常」，就大膽拋出讓 Kafka 重試
//            log.error("【搶票結果處理異常】資料庫或系統運作故障，將觸發 Kafka 重試機制。message: {}", message, e);
//            throw new RuntimeException("處理搶票結果失敗: " + e.getMessage(), e);
//        }
//    }

/**
 * Kafka Streams 結果 Payload（與 ReservationStreamProcessor.buildResult 對應）
 */
//    private static class ResultPayload {
//        public String orderId;
//        public boolean success;
//        public String message;
//    }
//}

//不使用自訂factory的版本
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class OrderResultConsumer {
//
//    private final OrderService orderService;
//    private final AreaService areaService;
//
//    /**
//     * 監聽搶票結果 Topic，並自動將 Value 轉換為 ReserveResult 物件
//     */
//    @KafkaListener(
//            topics = KafkaTopics.RESERVE_RESULT,
//            groupId = "${spring.kafka.consumer.group-id}" // 自動讀取 properties 的群組設定
//    )
//    public void consumeResult(ConsumerRecord<String, ReserveResult> record) {
//
//        if (record.value() == null) {
//            // 嘗試從 Header 撈出反序列化失敗的詳細資訊
//            Object exceptionHeader = record.headers().lastHeader("springDeserializerExceptionValue");
//
//            if (exceptionHeader != null) {
//                log.error("【❌ 偵測到毒藥訊息】此訊息 JSON 格式嚴重破損，已被防護罩攔截！Offset: {}", record.offset());
//                return;
//            }
//        }
//
//        // 2. 正常取出自動還原好的 Java 物件
//        ReserveResult result = record.value();
//        String kafkaKey = record.key(); // 這裏拿到的依然是最純粹的 String Key (例如: 1_B區)
//
//        log.info("【📥 收到搶票結果】Key: {}, 訂單號: {}, 是否成功: {}, 訊息: {}",
//                kafkaKey, result.getOrderId(), result.isSuccess(), result.getMessage());
//
//        // 3. 商業邏輯防禦：檢查是否為 KStreams 拋出的未知系統異常
//        if ("unknown".equals(result.getOrderId())) {
//            log.warn("【⚠️ 異常訂單】收到來自上游 KStreams 處理失敗的未知訂單，略過後續商業邏輯。原因: {}", result.getMessage());
//            return;
//        }
//
//        // =================================================================
//        // 4. 🚀 這裡開始執行你原本的商業邏輯 (例如：修改 MySQL 訂單狀態、發送 Websocket 通知前端...)
//        // =================================================================
//        // 若搶票成功，同步扣減 area 表的 qty（確保 SQLDB 與 RocksDB 最終一致）
//        try {
//            Integer orderId;
//            try {
//                orderId = Integer.valueOf(result.getOrderId());
//            } catch (NumberFormatException e) {
//                log.error("【搶票結果處理中斷】訂單號格式錯誤: {}，略過處理。", result.getOrderId());
//                return;
//            }
//
//            OrderStatus finalStatus = result.isSuccess() ? OrderStatus.SUCCESS : OrderStatus.FAILED;
//
//            // 1. 更新資料庫訂單狀態為最終狀態（SUCCESS or FAILED） 相同orderId的訂單都會一起修改狀態
//            orderService.update(
//                    null,
//                    new LambdaUpdateWrapper<MyBatisPlusOrdersEntity>()
//                            .set(MyBatisPlusOrdersEntity::getOrderStatus, finalStatus.getCode())
//                            .eq(MyBatisPlusOrdersEntity::getOrderId, orderId)
//            );
//            log.info("【訂單狀態更新】orderId: {} → {}", orderId, finalStatus.getDescription());
//
//            // 2. 若搶票成功，同步扣減 area 表的 qty（確保 SQLDB 與 RocksDB 最終一致）
//
//            if (result.isSuccess()) {
//
//                List<MyBatisPlusOrdersEntity> orders =
//                        result.getDetails()
//                                .stream()
//                                .map(detail -> {
//                                    MyBatisPlusOrdersEntity order = new MyBatisPlusOrdersEntity();
//
//                                    if (detail.getOrderId() != null) {
//                                        try {
//                                            order.setOrderId(Integer.valueOf(detail.getOrderId()));
//                                        } catch (NumberFormatException e) {
//                                            log.error("orderId 格式錯誤: {}", detail.getOrderId());
//                                        }
//                                    }
//
//                                    order.setEventsId(detail.getEventsId());
//                                    order.setOrderArea(detail.getOrderArea());
//                                    order.setOrderQty(detail.getQty());
//
//                                    return order;
//                                })
//                                .toList();
//
//                boolean synced = areaService.checkAndUpdateQty(orders);
//
//                if (synced) {
//                    log.info("【SQLDB 庫存同步成功】orderId: {}, 共 {} 筆座位",
//                            result.getOrderId(),
//                            orders.size());
//                } else {
//                    log.warn("【SQLDB 庫存同步失敗】orderId: {}",
//                            result.getOrderId());
//                }
//            }
//        } catch (Exception e) {
//            // 👈 這裡依然保留：如果是資料庫掛掉、連線逾時等「系統級異常」，就大膽拋出讓 Kafka 重試
//            log.error("【搶票結果處理異常】資料庫或系統運作故障，將觸發 Kafka 重試機制。message: ", e);
//            throw new RuntimeException("處理搶票結果失敗: " + e.getMessage(), e);
//        }
//    }
//}
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderResultConsumer {

    private final OrderService orderService;
    private final AreaService areaService;

    @KafkaListener(
            topics = KafkaTopics.RESERVE_RESULT,
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeResult(ConsumerRecord<String, ReserveResult> record) {

        ReserveResult result = record.value();

        // 1️⃣ poison message / null 防護
        if (result == null) {
            Object exceptionHeader = record.headers().lastHeader("springDeserializerExceptionValue");
            log.error("【❌ 毒消息】Offset: {}, header={}", record.offset(), exceptionHeader);
            return;
        }

        String kafkaKey = record.key();

        log.info("【📥 收到搶票結果】Key: {}, orderId: {}, success: {}",
                kafkaKey, result.getOrderId(), result.isSuccess());

        // 2️⃣ orderId 防呆
        final Integer orderId;
        try {
            if ("unknown".equals(result.getOrderId())) {
                log.warn("【⚠️ unknown orderId】略過，message={}", result.getMessage());
                return;
            }
            orderId = Integer.valueOf(result.getOrderId());
        } catch (Exception e) {
            log.error("【❌ orderId 格式錯誤】{}", result.getOrderId());
            return;
        }

        OrderStatus finalStatus =
                result.isSuccess() ? OrderStatus.SUCCESS : OrderStatus.FAILED;

        try {
            // 3️⃣ ⭐ 核心：用 DB 狀態做冪等（PROCESSING → FINAL）
            boolean affected = orderService.update(
                    null,
                    new LambdaUpdateWrapper<MyBatisPlusOrdersEntity>()
                            .set(MyBatisPlusOrdersEntity::getOrderStatus, finalStatus.getCode())
                            .eq(MyBatisPlusOrdersEntity::getOrderId, orderId)
                            .eq(MyBatisPlusOrdersEntity::getOrderStatus, OrderStatus.PROCESSING.getCode())
            );

            // 已經被處理過（SUCCESS / FAILED），直接跳過
            if (!affected ) {
                log.warn("【冪等保護】orderId: {} 已處理過，skip offset={}", orderId, record.offset());
                return;
            }

            // 4️⃣ success 才做庫存同步
            if (result.isSuccess()) {
                if (result.getDetails() == null || result.getDetails().isEmpty()) {
                    log.error("【資料異常】orderId: {} success 但 details 為空", orderId);
                    return;
                }
                var orders = result.getDetails()
                        .stream()
                        .map(detail -> {
                            MyBatisPlusOrdersEntity order = new MyBatisPlusOrdersEntity();
                            order.setOrderId(safeParse(detail.getOrderId()));
                            order.setEventsId(detail.getEventsId());
                            order.setOrderArea(detail.getOrderArea());
                            order.setOrderQty(detail.getQty());
                            return order;
                        })
                        .toList();
                boolean synced = areaService.checkAndUpdateQty(orders);

                if (synced) {
                    log.info("【庫存同步成功】orderId: {}, size={}", orderId, orders.size());
                } else {
                    log.error("【⚠️ 庫存不一致】orderId: {} SQL sync failed", orderId);
                    // 可接 alert
                }
            }
        } catch (Exception e) {
            log.error("【❌ Consumer 異常】orderId={}, will retry", orderId, e);
            throw new RuntimeException(e);
        }
    }

    private Integer safeParse(String id) {
        try {
            return Integer.valueOf(id);
        } catch (Exception e) {
            return null;
        }
    }
}