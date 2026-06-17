package com.concer.backend.kafka.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    // 1. 定義自定義的執行緒池
    @Bean(name = "kafkaConsumerExecutor")
    public ThreadPoolTaskExecutor kafkaConsumerExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);          // 核心執行緒數：同時有 3 個執行緒在跑
        executor.setMaxPoolSize(6);           // 最大執行緒數
        executor.setQueueCapacity(50);        // 緩衝佇列容量
        executor.setThreadNamePrefix("OrderConsumer-Th-"); // 執行緒名稱前綴，方便看 Log 查錯
        executor.initialize();
        return executor;
    }

    // 2. 建立 Consumer 基礎配置，嚴格指定 Key/Value 都用 StringDeserializer
    @Bean
    public ConsumerFactory<String, String> stringConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // 也可以在這裡加上自動重試、隔離等進階參數
        return new DefaultKafkaConsumerFactory<>(props);
    }

    // 3. ⭐ 產生對外的 Factory Bean，並把執行緒池與 String 配置整合進去
    @Bean(name = "stringKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> stringKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        // 綁定上面的 String 序列化配置
        factory.setConsumerFactory(stringConsumerFactory());

        // 👈 核心整合：將你自定義的多執行緒馬達 注入到這個工廠中
        factory.getContainerProperties().setListenerTaskExecutor(kafkaConsumerExecutor());

        // 設定預設的錯誤處理器
        factory.setCommonErrorHandler(new DefaultErrorHandler());

        return factory;
    }
}
