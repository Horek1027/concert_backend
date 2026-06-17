package com.concer.backend.kafka;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kv-debug")
public class KafkaStreamsDebugController {

    @Autowired
    private StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    @GetMapping("/store/{storeName}/{key}")
    public ResponseEntity<String> getStoreValue(@PathVariable String storeName, @PathVariable String key) {
        try {
            // 1. 取得 KafkaStreams 實例
            KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();

            // 2. 根據 Store 名稱取得唯讀的 RocksDB 門戶
            ReadOnlyKeyValueStore<String, String> store = kafkaStreams.store(
                    StoreQueryParameters.fromNameAndType(storeName, QueryableStoreTypes.keyValueStore())
            );

            // 3. 直接查詢 Key
            String value = store.get(key);
            return ResponseEntity.ok(value != null ? value : "Key 不存在");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("查詢失敗: " + e.getMessage());
        }
    }
}
