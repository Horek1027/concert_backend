package com.concer.backend.kafka;

import com.concer.backend.Request.OrderAddRequest;
import com.concer.backend.kafka.Event.OrderCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

//@Component
//public class OrderKafkaProducer {
//    private final KafkaTemplate<String, List<OrderAddRequest>> kafkaTemplate;
//
//    public OrderKafkaProducer(KafkaTemplate<String, List<OrderAddRequest>> kafkaTemplate) {
//        this.kafkaTemplate = kafkaTemplate;
//    }
//
//    public void sendOrderCreateMessage(List<OrderAddRequest> req) {
//        kafkaTemplate.send(KafkaTopics.ORDER_CREATE, req);
//    }
//}

@Component
public class OrderKafkaProducer {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public OrderKafkaProducer(
            KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderCreateMessage(String key, OrderCreatedEvent event) {
        // 👈 修改點：將 key 傳入 send 方法中，Kafka 會自動對 key 做 Hash 路由到特定 Partition
        kafkaTemplate.send(
                KafkaTopics.ORDER_CREATE,
                key,
                event
        );
    }
}
