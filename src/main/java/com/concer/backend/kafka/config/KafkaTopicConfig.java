package com.concer.backend.kafka.config;

import com.concer.backend.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // 1. 自動建立搶票「請求」Topic
    @Bean
    public NewTopic reserveRequestTopic() {
        return TopicBuilder.name(KafkaTopics.RESERVE_REQUEST) // 👈 直接用你的設定！
                .partitions(3) // 搶票高並發，建議至少 3 個分割區
                .replicas(1)
                .build();
    }

    // 2. 自動建立搶票「結果」Topic（Streams 處理完吐回來的結果）
    @Bean
    public NewTopic reserveResultTopic() {
        return TopicBuilder.name(KafkaTopics.RESERVE_RESULT) // 👈 順便把結果也建好
                .partitions(3)
                .replicas(1)
                .build();
    }
}