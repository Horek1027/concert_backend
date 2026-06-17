package com.concer.backend.kafka;

public class KafkaTopics {
    public static final String ORDER_CREATE = "concert.order.create";

    // Kafka Streams 搶票請求主題，存放SQLDB 資料
    public static final String RESERVE_REQUEST = "concert.reserve.request";
    // Kafka Streams 搶票結果主題
    public static final String RESERVE_RESULT = "concert.reserve.result";

    // 練習新增多個獨立的 RocksDB 狀態庫（State Store）
    public static final String USER_SYNC = "concert.user.sync";
}