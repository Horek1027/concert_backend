package com.concer.backend.kafka.config;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafkaStreams // 啟用 Spring Boot 對 Kafka Streams 的支持
public class KafkaStreamsConfig {
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public KafkaStreamsConfiguration kStreamsConfigs() {
        Map<String, Object> props = new HashMap<>();

        // 應用程式識別碼，同一 App 的所有實例共用，用於 consumer group 分配
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "concert-reservation-streams");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // 預設序列化/反序列化使用 String
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
                Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
                Serdes.String().getClass().getName());

        // 本地 RocksDB 狀態庫的實體儲存目錄（會在此目錄下產生 kafka-streams-state 資料夾）
        props.put(StreamsConfig.STATE_DIR_CONFIG, "./kafka-streams-state");

        // 使用 Exactly-Once Semantics V2 保證不重複扣減庫存（對 Kafka 3.0+ 有效）
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG,
                StreamsConfig.EXACTLY_ONCE_V2);

        // 每 1000ms 進行一次 commit，控制狀態同步至 Changelog Topic 的頻率
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);

        return new KafkaStreamsConfiguration(props);
    }
}
