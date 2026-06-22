package com.concer.backend.kafka.streams;

import com.concer.backend.kafka.DTO.OrderAggregate;
import com.concer.backend.kafka.DTO.ReserveRequest;
import com.concer.backend.kafka.DTO.ReserveResult;
import com.concer.backend.kafka.DTO.UserSyncRequest;
import com.concer.backend.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka Streams 搶票庫存扣減拓撲處理器
 * <p>
 * 核心設計：
 * 1. 監聽 INVENTORY_INIT 主題 → 將初始庫存寫入本地 RocksDB
 * 2. 監聽 RESERVE_REQUEST 主題 → 從 RocksDB 讀取並原子性扣減庫存
 * 3. 結果輸出至 RESERVE_RESULT 主題 → 由 OrderResultConsumer 異步更新資料庫
 * </p>
 * <p>
 * 同一 eventsId_orderArea 的所有請求在 Kafka 中被路由到同一個 Partition，
 * 由單一執行緒循序消費，完全無行鎖競爭。
 * </p>
 * //
 */
//@Component
//@Slf4j
//public class ReservationStreamProcessor {
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    /** RocksDB 狀態庫名稱，Key: eventsId_orderArea, Value: 剩餘庫存 */
//    public static final String INVENTORY_STORE = "area-inventory-store";
//
//    /**
//     * 定義嵌入式 RocksDB 持久化 KeyValueStore。
//     * Spring 啟動時由 Kafka Streams 框架自動創建並掛載至本地磁碟。
//     */
//    @Bean
//    public StoreBuilder<KeyValueStore<String, Integer>> inventoryStoreBuilder() {
//        return Stores.keyValueStoreBuilder(
//                Stores.persistentKeyValueStore(INVENTORY_STORE),
//                Serdes.String(),  // Key: "eventsId_orderArea" 例如 "101_A區"
//                Serdes.Integer()  // Value: 剩餘庫存數量
//        );
//    }
//
//    /**
//     * 宣告串流拓撲 (Topology)。
//     * 包含：庫存初始化流 + 搶票扣減流。
//     */
//    @Bean
//    public KStream<String, String> reservationKStream(StreamsBuilder kStreamBuilder) {
//
//        // 掛載狀態庫
//        kStreamBuilder.addStateStore(inventoryStoreBuilder());
//
//        // ─────────────────────────────────────────────────────
//        // 1. 庫存初始化流：監聽 INVENTORY_INIT → 寫入 RocksDB
//        //    啟動預熱時，StreamInventoryInitializer 會發送各區域庫存至此主題
//        // ─────────────────────────────────────────────────────
////        kStreamBuilder.stream(
////                KafkaTopics.INVENTORY_INIT,
////                Consumed.with(Serdes.String(), Serdes.String())
////        ).foreach((key, value) -> {
////            log.info("【RocksDB 初始化】Key: {}, 庫存: {}", key, value);
////        });
//        // 注意：真正的寫入由 Processor API 處理；此處以 peek 呈現日誌，
//        // 實際寫入在下方 transformValues 的 init() 中以 INIT 訊息格式觸發
//
//        // ─────────────────────────────────────────────────────
//        // 2. 搶票請求流：監聽 RESERVE_REQUEST → RocksDB 扣減 → 輸出至 RESERVE_RESULT
//        //    Key = eventsId_orderArea，確保同區域請求在同 Partition 循序執行
//        // ─────────────────────────────────────────────────────
//        KStream<String, String> requestStream = kStreamBuilder.stream(
//                KafkaTopics.RESERVE_REQUEST,
//                Consumed.with(Serdes.String(), Serdes.String())
//        );
//
//        KStream<String, String> resultStream = requestStream.transformValues(
//                () -> new ValueTransformerWithKey<String, String, String>() {
//
//                    private KeyValueStore<String, Integer> store;
//
//                    @Override
//                    @SuppressWarnings("unchecked")
//                    public void init(ProcessorContext context) {
//                        // 取得本地狀態庫控制權，後續讀寫直接操作磁碟/記憶體，無網路 I/O
//                        this.store = (KeyValueStore<String, Integer>)
//                                context.getStateStore(INVENTORY_STORE);
//                    }
//
//                    @Override
//                    public String transform(String key, String value) {
//                        try {
//                            StreamRequest req = objectMapper.readValue(value, StreamRequest.class);
//
//                            // 若是初始化指令，直接寫入 RocksDB 庫存
//                            if ("INIT".equals(req.action)) {
//                                store.put(key, req.qty);
//                                log.info("【RocksDB 寫入庫存】Key: {}, 初始庫存: {}", key, req.qty);
//                                return null; // 初始化不產生結果事件
//                            }
//
//                            // 從本地 RocksDB 讀取目前剩餘庫存（記憶體等級速度）
//                            Integer currentQty = store.get(key);
//
//                            if (currentQty == null) {
//                                log.warn("【RocksDB 查無庫存】Key: {} 尚未初始化", key);
//                                return buildResult(req.orderId, false, "區域庫存未初始化，請聯繫管理員");
//                            }
//
//                            if (currentQty >= req.qty) {
//                                // 庫存足夠：原子性本地扣減
//                                int newQty = currentQty - req.qty;
//                                store.put(key, newQty);
//                                log.info("【RocksDB 扣減成功】Key: {}, 扣前: {}, 扣後: {}", key, currentQty, newQty);
//                                return buildResult(req.orderId, true, "搶票成功");
//                            } else {
//                                log.warn("【RocksDB 庫存不足】Key: {}, 現有: {}, 需求: {}", key, currentQty, req.qty);
//                                return buildResult(req.orderId, false, "座位庫存不足");
//                            }
//
//                        } catch (Exception e) {
//                            log.error("【RocksDB 處理異常】", e);
//                            return String.format("{\"orderId\":\"unknown\",\"success\":false,\"message\":\"%s\"}", e.getMessage());
//                        }
//                    }
//
//                    @Override
//                    public void close() {}
//                },
//                INVENTORY_STORE
//        );
//
//        // 過濾掉初始化指令產生的 null 結果，只輸出扣減結果
//        resultStream
//                .filter((key, value) -> value != null)
//                .to(KafkaTopics.RESERVE_RESULT, Produced.with(Serdes.String(), Serdes.String()));
//
//        return resultStream;
//    }
//
//    private String buildResult(String orderId, boolean success, String message) {
//        return String.format("{\"orderId\":\"%s\",\"success\":%b,\"message\":\"%s\"}",
//                orderId, success, message);
//    }
//
//    /**
//     * 搶票請求 Payload（JSON 反序列化用）
//     * 由 OrderServiceImpl.insertFromKafka 序列化後發送
//     */
//    public static class StreamRequest {
//        public String orderId;  // 訂單明細 ID
//        public int qty;         // 購買數量
//        public String action;   // "LOCK"（搶票）或 "INIT"（初始化庫存）
//    }
//}


/**
 * Kafka Streams 搶票庫存扣減拓撲處理器
 * <p>
 * 核心設計：
 * 1. 監聽 RESERVE_REQUEST 主題 → 處理「INIT(初始化庫存)」與「LOCK(扣減庫存)」指令。
 * 2. 結果輸出至 RESERVE_RESULT 主題 → 由 OrderResultConsumer 異步更新資料庫。
 * </p>
 */
//@Component
//@Slf4j
//public class ReservationStreamProcessor {
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    /** RocksDB 狀態庫名稱，Key: eventsId_orderArea, Value: 剩餘庫存 */
//    public static final String INVENTORY_STORE = "area-inventory-store";
//
//    @Bean
//    public StoreBuilder<KeyValueStore<String, Integer>> inventoryStoreBuilder() {
//        return Stores.keyValueStoreBuilder(
//                Stores.persistentKeyValueStore(INVENTORY_STORE),
//                Serdes.String(),
//                Serdes.Integer()
//        );
//    }
//
//    @Bean
//    public KStream<String, String> reservationKStream(StreamsBuilder kStreamBuilder) {
//
//        // 掛載狀態庫
//        kStreamBuilder.addStateStore(inventoryStoreBuilder());
//
//        // 監聽單一入口：RESERVE_REQUEST
//        KStream<String, String> requestStream = kStreamBuilder.stream(
//                KafkaTopics.RESERVE_REQUEST,
//                Consumed.with(Serdes.String(), Serdes.String())
//        );
//
//        KStream<String, String> resultStream = requestStream.transformValues(
//                () -> new ValueTransformerWithKey<String, String, String>() {
//
//                    private KeyValueStore<String, Integer> store;
//
//                    @Override
//                    @SuppressWarnings("unchecked")
//                    public void init(ProcessorContext context) {
//                        this.store = (KeyValueStore<String, Integer>) context.getStateStore(INVENTORY_STORE);
//                    }
//
//                    @Override
//                    public String transform(String key, String value) {
//                        try {
//                            // 👈 ⭐ 關鍵修正點：防禦雙重序列化字串
//                            StreamRequest req;
//                            try {
//                                req = objectMapper.readValue(value, StreamRequest.class);
//                            } catch (MismatchedInputException e) {
//                                log.warn("【⚠️ 偵測到雙重序列化字串】上游資料格式異常，啟動二次解構防禦機制...");
//                                // 先解開外層的字串包裝，還原成標準 JSON 字串
//                                String unescapedJson = objectMapper.readValue(value, String.class);
//                                // 再解析成實體物件
//                                req = objectMapper.readValue(unescapedJson, StreamRequest.class);
//                            }
//
//                            // 1. 處理初始化指令 (由 StreamInventoryInitializer 觸發)
//                            if ("INIT".equals(req.action)) {
//                                store.put(key, req.qty);
//                                log.info("【RocksDB 寫入庫存】Key: {}, 初始庫存: {}", key, req.qty);
//                                return null; // 初始化不產生結果事件
//                            }
//
//                            // 2. 處理搶票扣減指令 (由 insertFromKafka 觸發)
//                            Integer currentQty = store.get(key);
//
//                            if (currentQty == null) {
//                                log.warn("【RocksDB 查無庫存】Key: {} 尚未初始化", key);
//                                return buildResult(req.orderId, false, "區域庫存未初始化，請聯繫管理員");
//                            }
//
//                            if (currentQty >= req.qty) {
//                                // 庫存足夠：原子性本地扣減
//                                int newQty = currentQty - req.qty;
//                                store.put(key, newQty);
//                                log.info("【RocksDB 扣減成功】Key: {}, 扣前: {}, 扣後: {}", key, currentQty, newQty);
//                                return buildResult(req.orderId, true, "搶票成功");
//                            } else {
//                                log.warn("【RocksDB 庫存不足】Key: {}, 現有: {}, 需求: {}", key, currentQty, req.qty);
//                                return buildResult(req.orderId, false, "座位庫存不足");
//                            }
//
//                        } catch (Exception e) {
//                            log.error("【RocksDB 處理異常】", e);
//                            // 👈 修正點：發生未知異常時，統一呼叫修正後的 buildResult，帶入 "unknown"
//                            return buildResult("unknown", false, "系統處理異常: " + e.getMessage());
//                        }
//                    }
//
//                    @Override
//                    public void close() {}
//                },
//                INVENTORY_STORE
//        );
//        // 過濾掉初始化指令產生的 null 結果，只輸出搶票扣減結果
//        resultStream
//                .filter((key, value) -> value != null)
//                .to(KafkaTopics.RESERVE_RESULT, Produced.with(Serdes.String(), Serdes.String()));
//
//        return resultStream;
//    }

//放棄使用自訂factory 的版本
//@Component
//@Slf4j
//public class ReservationStreamProcessor {
//    public static final String INVENTORY_STORE = "area-inventory-store";
//    // 💡 1. 宣告第二個 RocksDB 名稱這邊用不到
//    public static final String USER_STORE = "user-state-store";
//
//    @Bean
//    public KStream<String, ReserveResult> reservationKStream(StreamsBuilder kStreamBuilder) {
//        // 💡 建立 KStreams 專用的 JSON 序列化器
//        JsonSerde<ReserveRequest> requestSerde = new JsonSerde<>(ReserveRequest.class);
//        JsonSerde<ReserveResult> resultSerde = new JsonSerde<>(ReserveResult.class);
//
//        kStreamBuilder.addStateStore(Stores.keyValueStoreBuilder(
//                Stores.persistentKeyValueStore(INVENTORY_STORE), Serdes.String(), Serdes.Integer()));
//
//        // 💡 讀取時，指定使用 requestSerde
//        KStream<String, ReserveRequest> requestStream = kStreamBuilder.stream(
//                KafkaTopics.RESERVE_REQUEST,
//                Consumed.with(Serdes.String(), requestSerde)
//        );
//
//        // 💡 transformValues 處理的輸入與輸出都變成實體 Object 了！
//        KStream<String, ReserveResult> resultStream = requestStream.processValues(
//                () -> new FixedKeyProcessor<String, ReserveRequest, ReserveResult>() {
//
//                    private KeyValueStore<String, Integer> store;
//                    // 💡 新版改用 FixedKeyProcessorContext
//                    private FixedKeyProcessorContext<String, ReserveResult> ctx;
//
//                    @Override
//                    public void init(FixedKeyProcessorContext<String, ReserveResult> context) {
//                        this.ctx = context;
//                        this.store = context.getStateStore(INVENTORY_STORE);
//                    }
//
//                    @Override
//                    public void process(FixedKeyRecord<String, ReserveRequest> record) {
//                        String key = record.key();
//                        ReserveRequest req = record.value();
//
//                        if (req == null || req.getAction() == null) return;
//
//                        Integer currentQty = store.get(key);
//
//                        // 💡 內部的 switch-case 邏輯完全不需要變動！
//                        switch (req.getAction()) {
//                            case "INIT":
//                                store.put(key, req.getQty());
//                                break;
//
//                            case "LOCK":
//                                if (currentQty == null) {
//                                    ctx.forward(record.withValue(new ReserveResult(
//                                            req.getOrderId(),
//                                            req.getEventsId(),  // 從 request 提取並往後傳遞
//                                            req.getOrderArea(), // 從 request 提取並往後傳遞
//                                            req.getQty(),       // 從 request 提取並往後傳遞
//                                            false,
//                                            "區域庫存未初始化"
//                                    )));
//                                    break;
//                                }
//                                if (currentQty >= req.getQty()) {
//                                    store.put(key, currentQty - req.getQty());
//                                    ctx.forward(record.withValue(new ReserveResult(
//                                            req.getOrderId(),
//                                            req.getEventsId(),
//                                            req.getOrderArea(),
//                                            req.getQty(),
//                                            true,
//                                            "搶票成功"
//                                    )));
//                                } else {
//                                    ctx.forward(record.withValue(new ReserveResult(
//                                            req.getOrderId(),
//                                            req.getEventsId(),
//                                            req.getOrderArea(),
//                                            req.getQty(),
//                                            false,
//                                            "座位庫存不足"
//                                    )));
//                                }
//                                break;
//
//                            case "RELEASE":
//                                if (currentQty == null) {
//                                    ctx.forward(record.withValue(new ReserveResult(
//                                            req.getOrderId(),
//                                            req.getEventsId(),
//                                            req.getOrderArea(),
//                                            req.getQty(),
//                                            false,
//                                            "系統異常：無法釋放"
//                                    )));
//                                    break;
//                                }
//                                store.put(key, currentQty + req.getQty());
//                                ctx.forward(record.withValue(new ReserveResult(
//                                        req.getOrderId(),
//                                        req.getEventsId(),
//                                        req.getOrderArea(),
//                                        req.getQty(),
//                                        true,
//                                        "庫存已釋放回寫"
//                                )));
//                                break;
//                        }
//                    }
//                    @Override
//                    public void close() {
//                        // 必須保留空的實作
//                    }
//                },
//                INVENTORY_STORE
//        );
//        // 💡 寫出時，指定使用 resultSerde
//        resultStream
//                .filter((key, value) -> value != null)
//                .to(KafkaTopics.RESERVE_RESULT, Produced.with(Serdes.String(), resultSerde));
//        return resultStream;
//    }
//}

//  //多RocksDB 用.proccess 處理
//@Component
//@Slf4j
//public class ReservationStreamProcessor {
//
//    public static final String INVENTORY_STORE = "area-inventory-store";
//    public static final String USER_STORE = "user-state-store"; // 💡 1. 宣告第二個 RocksDB 名稱
//
//    @Bean
//    public KStream<String, ReserveResult> reservationKStream(StreamsBuilder kStreamBuilder) {
//
//        JsonSerde<ReserveRequest> requestSerde = new JsonSerde<>(ReserveRequest.class);
//        JsonSerde<ReserveResult> resultSerde = new JsonSerde<>(ReserveResult.class);
//        JsonSerde<UserSyncRequest> userSerde = new JsonSerde<>(UserSyncRequest.class); // 💡 新增 User 序列化器
//
//        // 💡 2. 註冊第二個狀態庫（Key 是 String, Value 是 UserSyncRequest 物件）
//        kStreamBuilder.addStateStore(Stores.keyValueStoreBuilder(
//                Stores.persistentKeyValueStore(INVENTORY_STORE), Serdes.String(), Serdes.Integer()));
//
//        kStreamBuilder.addStateStore(Stores.keyValueStoreBuilder(
//                Stores.persistentKeyValueStore(USER_STORE), Serdes.String(), userSerde));
//
//// ----------------------------------------------------
//// 支線任務：監聽 User Topic，默默把資料同步進 USER_STORE
//// ----------------------------------------------------
//        kStreamBuilder.stream(KafkaTopics.USER_SYNC, Consumed.with(Serdes.String(), userSerde))
//                .process(() -> new Processor<String, UserSyncRequest, Void, Void>() {
//
//                    private KeyValueStore<String, UserSyncRequest> userStore;
//
//                    @Override
//                    public void init(org.apache.kafka.streams.processor.api.ProcessorContext<Void, Void> context) {
//                        this.userStore = context.getStateStore(USER_STORE);
//                    }
//
//                    @Override
//                    public void process(org.apache.kafka.streams.processor.api.Record<String, UserSyncRequest> record) {
//                        // 🎯 這裡的 Record<String, UserSyncRequest> 必須與上面 Processor<String, UserSyncRequest...> 完全一致！
//                        if (record.value() != null && "INIT".equals(record.value().getAction())) {
//                            userStore.put(record.key(), record.value());
//                        }
//                    }
//
//                    @Override
//                    public void close() {
//                        // 💡 新版的 Processor 必須實作或保留 close() 方法的空實作
//                    }
//                }, USER_STORE);
//
//        // ----------------------------------------------------
//        // 主線任務：處理搶票請求，同時操作多個 RocksDB Store
//        // ----------------------------------------------------
//        KStream<String, ReserveRequest> requestStream = kStreamBuilder.stream(
//                KafkaTopics.RESERVE_REQUEST, Consumed.with(Serdes.String(), requestSerde));
//
//        // 💡 關鍵：改用 .process()，並同時傳入 INVENTORY_STORE 與 USER_STORE 兩個名字！
//        KStream<String, ReserveResult> resultStream = requestStream.process(
//                () -> new org.apache.kafka.streams.processor.api.Processor<String, ReserveRequest, String, ReserveResult>() {
//
//                    private KeyValueStore<String, Integer> inventoryStore;
//                    private KeyValueStore<String, UserSyncRequest> userStore;
//                    private org.apache.kafka.streams.processor.api.ProcessorContext<String, ReserveResult> ctx;
//
//                    @Override
//                    public void init(org.apache.kafka.streams.processor.api.ProcessorContext<String, ReserveResult> context) {
//                        this.ctx = context;
//                        // 同時初始化拿取兩個狀態庫
//                        this.inventoryStore = context.getStateStore(INVENTORY_STORE);
//                        this.userStore = context.getStateStore(USER_STORE);
//                    }
//
//                    @Override
//                    public void process(org.apache.kafka.streams.processor.api.Record<String, ReserveRequest> record) {
//                        String key = record.key();
//                        ReserveRequest req = record.value();
//
//                        if (req == null) return;
//
//                        if ("INIT".equals(req.getAction())) {
//                            inventoryStore.put(key, req.getQty());
//                            return;
//                        }
//
//                        // 交叉查詢多個 Store 練習
//                        String userId = "2"; // 模擬資料
//                        UserSyncRequest user = userStore.get(userId);
//                        //user.getStatus() == 2 表示黑名單
//                        if (user != null &&  user.getStatus() == 2 ) {
//                            // 黑名單直接攔截，轉發失敗結果
//                            ctx.forward(new org.apache.kafka.streams.processor.api.Record<>(
//                                    key,
//                                    new ReserveResult(req.getOrderId(), false, "黃牛帳號，拒絕購票"),
//                                    record.timestamp()
//                            ));
//                            return;
//                        }
//
//                        // 第一個 Store 的庫存扣減
//                        Integer currentQty = inventoryStore.get(key);
//                        if (currentQty != null && currentQty >= req.getQty()) {
//                            inventoryStore.put(key, currentQty - req.getQty());
//                            ctx.forward(new org.apache.kafka.streams.processor.api.Record<>(
//                                    key,
//                                    new ReserveResult(req.getOrderId(), true, "搶票成功"),
//                                    record.timestamp()
//                            ));
//                        } else {
//                            ctx.forward(new org.apache.kafka.streams.processor.api.Record<>(
//                                    key,
//                                    new ReserveResult(req.getOrderId(), false, "庫存不足"),
//                                    record.timestamp()
//                            ));
//                        }
//                    }
//                    @Override
//                    public void close() {
//                    }
//                },
//                INVENTORY_STORE, USER_STORE
//        );
//
//        // 寫出結果主題
//        resultStream.to(KafkaTopics.RESERVE_RESULT, Produced.with(Serdes.String(), resultSerde));
//
//        return resultStream;
//    }
//}
//
@Component
@Slf4j
// 【修改】注入 KafkaTemplate，用於在 finalStream 失敗時發送 RELEASE 補償訊息
@RequiredArgsConstructor
public class ReservationStreamProcessor {
    /** RocksDB 狀態庫名稱，Key: eventsId_orderArea, Value: 剩餘庫存 */
    public static final String INVENTORY_STORE = "area-inventory-store";
    // 【修改】新增：注入 KafkaTemplate，讓 KStream 可以在部分失敗時主動發出 RELEASE 補償訊息
    // 原因：Consumer 層只負責業務邏輯，不應該知道哪些 area 曾經成功 LOCK，補償責任應在 KStream 層
    private final KafkaTemplate<String, ReserveRequest> kafkaTemplate;
    @Bean
    public KStream<String, ReserveResult> reservationKStream(StreamsBuilder builder) {
        JsonSerde<ReserveRequest> requestSerde = new JsonSerde<>(ReserveRequest.class);
        JsonSerde<ReserveResult> resultSerde = new JsonSerde<>(ReserveResult.class);
        JsonSerde<OrderAggregate> aggregateSerde = new JsonSerde<>(OrderAggregate.class);

        builder.addStateStore(
                Stores.keyValueStoreBuilder(
                        Stores.persistentKeyValueStore(INVENTORY_STORE),
                        Serdes.String(),
                        Serdes.Integer()
                )
        );
        // 1. 讀取 LOCK / INIT / RELEASE 請求
        KStream<String, ReserveRequest> requestStream =
                builder.stream(
                        KafkaTopics.RESERVE_REQUEST, // 這邊設定監聽的主題
                        Consumed.with(Serdes.String(), requestSerde)
                );
        // 2. 第一層：庫存檢查（每筆都跑）
        KStream<String, ReserveResult> resultStream =
                requestStream.processValues(
                        () -> new FixedKeyProcessor<String, ReserveRequest, ReserveResult>() {
                            private KeyValueStore<String, Integer> store;
                            private FixedKeyProcessorContext<String, ReserveResult> ctx;
                            @Override
                            public void init(FixedKeyProcessorContext<String, ReserveResult> context) {
                                this.ctx = context;
                                this.store = context.getStateStore(INVENTORY_STORE);
                            }
                            @Override
                            public void process(FixedKeyRecord<String, ReserveRequest> record) {
                                String key = record.key();
                                ReserveRequest req = record.value();

                                if (req == null || req.getAction() == null) return;

                                Integer currentQty = store.get(key);

                                switch (req.getAction()) {
                                    case "INIT":
                                        store.put(key, req.getQty());
                                        // 【修改】新增 log，方便追蹤庫存初始化時機
                                        // 原因：INIT 若晚於 LOCK 到達，currentQty 會是 null 導致訂單直接失敗，
                                        //  加 log 可快速定位時序問題
                                        log.info("【庫存初始化】key: {}, 初始數量: {}", key, req.getQty());
                                        break;
                                    case "LOCK":
                                        if (currentQty == null) {
                                            // 【修改】新增 warn log，協助排查 INIT 未到達的時序問題
                                            log.warn("【LOCK 失敗】key: {} 庫存尚未初始化，orderId: {}", key, req.getOrderId());
                                            ctx.forward(record.withValue(
                                                    new ReserveResult(
                                                            req.getOrderId(),
                                                            req.getEventsId(),
                                                            req.getOrderArea(),
                                                            req.getQty(),
                                                            false,
                                                            "區域庫存未初始化",
                                                            req.getTotalSegments(),
                                                            null
                                                    )
                                            ));
                                            break;
                                        }
                                        if (currentQty >= req.getQty()) {
                                            store.put(key, currentQty - req.getQty());

                                            ctx.forward(record.withValue(
                                                    new ReserveResult(
                                                            req.getOrderId(),
                                                            req.getEventsId(),
                                                            req.getOrderArea(),
                                                            req.getQty(),
                                                            true,
                                                            "搶票成功",
                                                            req.getTotalSegments(),
                                                            null
                                                    )
                                            ));
                                        } else {
                                            ctx.forward(record.withValue(
                                                    new ReserveResult(
                                                            req.getOrderId(),
                                                            req.getEventsId(),
                                                            req.getOrderArea(),
                                                            req.getQty(),
                                                            false,
                                                            "庫存不足",
                                                            req.getTotalSegments(),
                                                            null
                                                    )
                                            ));
                                        }
                                        break;
                                    case "RELEASE":
                                        if (currentQty == null) {
                                            // 【修改】新增 warn log，補償訊息若發現 key 不存在屬於異常狀況
                                            log.warn("【RELEASE 失敗】key: {} 庫存狀態不存在，orderId: {}", key, req.getOrderId());
                                            ctx.forward(record.withValue(
                                                    new ReserveResult(
                                                            req.getOrderId(),
                                                            req.getEventsId(),
                                                            req.getOrderArea(),
                                                            req.getQty(),
                                                            false,
                                                            "無法釋放庫存",
                                                            req.getTotalSegments(),
                                                            null
                                                    )
                                            ));
                                            break;
                                        }
                                        store.put(key, currentQty + req.getQty());
                                        log.info("【庫存歸還】key: {}, 歸還數量: {}, 歸還後庫存: {}, orderId: {}",
                                                key, req.getQty(), currentQty + req.getQty(), req.getOrderId());
                                        ctx.forward(record.withValue(
                                                new ReserveResult(
                                                        req.getOrderId(),
                                                        req.getEventsId(),
                                                        req.getOrderArea(),
                                                        req.getQty(),
                                                        true,
                                                        "庫存已釋放",
                                                        req.getTotalSegments(),
                                                        null
                                                )
                                        ));
                                        break;
                                }
                            }
                            @Override
                            public void close() {
                            }
                        },
                        INVENTORY_STORE
                );
        // 3. 第二層：按 orderId 聚合
        // 【注意】RELEASE 補償訊息的 orderId 會帶 "RELEASE_" 前綴，
        //  所以補償結果不會被歸入原始訂單的 aggregate，兩者互不干擾
        KTable<String, OrderAggregate> aggTable =
                resultStream
                        .groupBy(
                                (key, value) -> value.getOrderId(),
                                Grouped.with(Serdes.String(), resultSerde)
                        )
                        .aggregate(
                                OrderAggregate::new,
                                (orderId, result, agg) -> {
                                    agg.add(result);
                                    return agg;
                                },
                                Materialized.with(
                                        Serdes.String(),
                                        aggregateSerde
                                )
                        );
        // 4. 完整訂單才輸出一次
        KStream<String, ReserveResult> finalStream =
                aggTable.toStream()
                        .filter((orderId, agg) -> agg != null && agg.isComplete())
                        .mapValues((orderId, agg) -> {
                            ReserveResult first = agg.getResults().get(0);
                            ReserveResult finalResult = new ReserveResult();
                            finalResult.setOrderId(first.getOrderId());
                            finalResult.setEventsId(first.getEventsId());
                            finalResult.setQty(
                                    agg.getResults()
                                            .stream()
                                            .mapToInt(ReserveResult::getQty)
                                            .sum()
                            );
                            finalResult.setDetails(agg.getResults());
                            if (agg.allSuccess()) {
                                // 全部成功，正常輸出
                                finalResult.setSuccess(true);
                                finalResult.setMessage("整筆訂單搶票成功");
                                log.info("【訂單完成】orderId: {} 全部搶票成功", orderId);
                            } else {
                                // 【修改】核心補償邏輯：部分或全部失敗時，找出已成功 LOCK 的 area，發 RELEASE 補償
                                // 原因：KStream processValues 對每筆訊息獨立處理，
                                // 若前兩筆 LOCK 成功、第三筆失敗，前兩筆的 RocksDB 庫存已被扣除，
                                // 若不補償將造成庫存永久洩漏，其他用戶永遠搶不到這些票
                                finalResult.setSuccess(false);
                                finalResult.setMessage("整筆訂單部分失敗");

                                List<ReserveResult> successResults = agg.getResults()
                                        .stream()
                                        .filter(ReserveResult::isSuccess)
                                        .toList();

                                if (!successResults.isEmpty()) {
                                    log.warn("【庫存補償啟動】orderId: {} 部分失敗，將對 {} 個已成功 LOCK 的區域發出 RELEASE",
                                            orderId, successResults.size());

                                    for (ReserveResult successResult : successResults) {
                                        ReserveRequest releaseReq = new ReserveRequest();

                                        //【修改】補償訊息的 orderId 加上 "RELEASE_" 前綴
                                        // 原因：若使用原始 orderId，RELEASE 結果會被 aggregate 收入同一個 OrderAggregate，
                                        // 可能使 isComplete() 判斷錯誤，或觸發第二次輸出到 RESERVE_RESULT，
                                        // 加前綴後 aggregate 會將其視為獨立訂單，結果不會流向 RESERVE_RESULT Consumer
                                        releaseReq.setOrderId("RELEASE_" + successResult.getOrderId());
                                        releaseReq.setEventsId(successResult.getEventsId());
                                        releaseReq.setOrderArea(successResult.getOrderArea());
                                        releaseReq.setQty(successResult.getQty());
                                        releaseReq.setAction("RELEASE");

                                        //【修改】補償訊息的 TotalSegments 設為 1
                                        // 原因：每筆 RELEASE 獨立完成，不需要等其他訊息聚合，
                                        // 設為 1 讓 isComplete() 在收到這唯一一筆時即為 true
                                        // 但因 orderId 帶前綴，這個 aggregate 結果不會被 filter 到 finalStream 輸出
                                        releaseReq.setTotalSegments(1);
                                        String releaseKey = successResult.getEventsId() + "_" + successResult.getOrderArea();
                                        kafkaTemplate.send(KafkaTopics.RESERVE_REQUEST, releaseKey, releaseReq);
                                        log.warn("【補償訊息已發送】RELEASE key: {}, qty: {}, 補償 orderId: {}",
                                                releaseKey, successResult.getQty(), releaseReq.getOrderId());
                                    }
                                } else {
                                    // 三筆全部失敗（例如全部庫存不足），沒有任何已 LOCK 的庫存需要歸還
                                    log.info("【無需補償】orderId: {} 所有區域均未成功 LOCK，無庫存洩漏", orderId);
                                }
                            }
                            return finalResult;
                        });
        // 5. 最終只吐一次到 Consumer
        finalStream.to(
                KafkaTopics.RESERVE_RESULT,
                Produced.with(Serdes.String(), resultSerde)
        );
        return finalStream;
    }
}