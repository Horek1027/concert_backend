package com.concer.backend.kafka.streams;

import com.concer.backend.kafka.DTO.ReserveRequest;
import com.concer.backend.kafka.DTO.ReserveResult;
import com.concer.backend.kafka.DTO.UserSyncRequest;
import com.concer.backend.kafka.KafkaTopics;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.ValueTransformerWithKey;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.serializer.JsonSerde;
import org.springframework.stereotype.Component;

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
//
//    private String buildResult(String orderId, boolean success, String message) {
//        try {
//            return objectMapper.createObjectNode()
//                    .put("orderId",orderId)
//                    .put("success", success)
//                    .put("message", message)
//                    .toString(); // 👈 自動生成完美轉義、無語法錯誤的 JSON 字串
//        } catch (Exception e) {
//            log.error("【嚴重錯誤】序列化回傳結果失敗", e);
////            return objectMapper.createObjectNode()
////                    .put("orderId","unknown")
////                    .put("success",false )
////                    .put("message", e.getMessage())
////                    .toString();
//            return "{\"orderId\":\"unknown\",\"success\":false,\"message\":\"Fatal String Serialization Error\"}";
//        }
//    }
//    @Data
//    @NoArgsConstructor  // 👈 必須要有這個，Jackson 才能順利 new 出物件！
//    @AllArgsConstructor
//    public static class StreamRequest {
//        public String orderId;
//        public int qty;
//        public String action;
//    }
//}


//放棄使用自訂factory 的版本
@Component
@Slf4j
public class ReservationStreamProcessor {
    public static final String INVENTORY_STORE = "area-inventory-store";
    // 💡 1. 宣告第二個 RocksDB 名稱這邊用不到
    public static final String USER_STORE = "user-state-store";

    @Bean
    public KStream<String, ReserveResult> reservationKStream(StreamsBuilder kStreamBuilder) {
        // 💡 建立 KStreams 專用的 JSON 序列化器
        JsonSerde<ReserveRequest> requestSerde = new JsonSerde<>(ReserveRequest.class);
        JsonSerde<ReserveResult> resultSerde = new JsonSerde<>(ReserveResult.class);

        kStreamBuilder.addStateStore(Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(INVENTORY_STORE), Serdes.String(), Serdes.Integer()));

        // 💡 讀取時，指定使用 requestSerde
        KStream<String, ReserveRequest> requestStream = kStreamBuilder.stream(
                KafkaTopics.RESERVE_REQUEST,
                Consumed.with(Serdes.String(), requestSerde)
        );

        // 💡 transformValues 處理的輸入與輸出都變成實體 Object 了！
        KStream<String, ReserveResult> resultStream = requestStream.processValues(
                () -> new FixedKeyProcessor<String, ReserveRequest, ReserveResult>() {

                    private KeyValueStore<String, Integer> store;
                    // 💡 新版改用 FixedKeyProcessorContext
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

                        // 💡 內部的 switch-case 邏輯完全不需要變動！
                        switch (req.getAction()) {
                            case "INIT":
                                store.put(key, req.getQty());
                                // 💡 新版發送結果到下游的語法：使用 ctx.forward
                                // 因為 INIT 不需要往下送，這裡什麼都不做，等同於原本的 return null;
                                break;

                            case "LOCK":
                                if (currentQty == null) {
                                    ctx.forward(record.withValue(new ReserveResult(req.getOrderId(), false, "區域庫存未初始化")));
                                    break;
                                }
                                if (currentQty >= req.getQty()) {
                                    store.put(key, currentQty - req.getQty());
                                    ctx.forward(record.withValue(new ReserveResult(req.getOrderId(), true, "搶票成功")));
                                } else {
                                    ctx.forward(record.withValue(new ReserveResult(req.getOrderId(), false, "座位庫存不足")));
                                }
                                break;

                            case "RELEASE":
                                if (currentQty == null) {
                                    ctx.forward(record.withValue(new ReserveResult(req.getOrderId(),
                                            false, "系統異常：無法釋放")));
                                    break;
                                }
                                store.put(key, currentQty + req.getQty());
                                ctx.forward(record.withValue(new ReserveResult(req.getOrderId(),
                                        true, "庫存已釋放回寫")));
                                break;
                        }
                    }
                    @Override
                    public void close() {
                        // 必須保留空的實作
                    }
                },
                INVENTORY_STORE
        );
        // 💡 寫出時，指定使用 resultSerde
        resultStream
                .filter((key, value) -> value != null)
                .to(KafkaTopics.RESERVE_RESULT, Produced.with(Serdes.String(), resultSerde));
        return resultStream;
    }
}

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
