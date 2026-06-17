package com.concer.backend.kafka;

import com.concer.backend.kafka.Event.OrderCreatedEvent;
import com.concer.backend.orders.Service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

//@Component
//@Slf4j
//@RequiredArgsConstructor // 👈 自動生成建構子，不用再手動寫了
//public class OrderKafkaConsumer {
//
//    private final OrderService orderService;
//
//    // 👈 建立專屬的 ObjectMapper，並註冊 JavaTimeModule 以便正確解析 LocalDateTime
//    private final ObjectMapper objectMapper = new ObjectMapper()
//            .registerModule(new JavaTimeModule());
//
//    @KafkaListener(
//            topics = KafkaTopics.ORDER_CREATE,
//            groupId = "concert-order-group",
//            // 👈 核心修正 1：指定工廠，立刻啟用 3~6 個執行緒池並行「進件寫入 DB」
//            containerFactory = "stringKafkaListenerContainerFactory"
//    )
//    public void consumeOrderCreateMessage(String message) { // 👈 核心修正 2：改收 String 確保不崩潰
//        try {
//            log.debug("【收到前端搶票進件】原始訊息: {}", message);
//
//            // 手動安全解析
//            OrderCreatedEvent event = objectMapper.readValue(message, OrderCreatedEvent.class);
//
//            // 呼叫 Service 執行資料庫批次寫入 (insertFromKafka)
//            orderService.insertFromKafka(event);
//
//        } catch (Exception e) {
//            // 👈 核心防護：記錄異常，但不拋出！防止毒藥訊息導致你的核心進件通道卡死
//            log.error("【前端進件處理異常】訊息解析或寫入失敗，該筆訂單遭捨棄。Message: {}", message, e);
//        }
//    }
//}

//不使用自己設定 factory 寫法
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderKafkaConsumer {
    private final OrderService orderService;
    @KafkaListener(
            topics = KafkaTopics.ORDER_CREATE,
            groupId = "${spring.kafka.consumer.group-id}" // 讀取全域 GroupId 設定
            // 💡 修正點 2：刪除 containerFactory，直接沿用全域的「防彈 JSON 工廠」
    )
    public void consumeOrderCreateMessage(ConsumerRecord<String, OrderCreatedEvent> record) {
        // 💡 修正點 3：加上全域防彈檢查（防止毒藥訊息卡死進件通道）
        if (record.value() == null) {
            if (record.headers().lastHeader("springDeserializerExceptionValue") != null) {
                log.error("【❌ 偵測到前端進件毒藥訊息】JSON 格式嚴重破損，已被防護罩攔截！Offset: {}, 略過此訊息。", record.offset());
                return;
            }
        }

        // 💡 修正點 4：直接拿到完美轉換好的實體物件，乾淨溜溜
        OrderCreatedEvent event = record.value();
        log.debug("【📥 收到前端搶票進件】UserId: {}, CorrelationId: {}", event.userId(), event.correlationId());

        try {
            // 呼叫 Service 執行資料庫批次寫入 (insertFromKafka)
            orderService.insertFromKafka(event);
        } catch (Exception e) {
            // 資料庫寫入異常的保護（例如 DB 斷線等商業邏輯異常）
            log.error("【❌ 前端進件寫入 DB 失敗】UserId: {}", event.userId(), e);
        }
    }
}