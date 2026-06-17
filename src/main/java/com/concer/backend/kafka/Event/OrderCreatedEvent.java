package com.concer.backend.kafka.Event;

import com.concer.backend.Request.OrderAddRequest;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record OrderCreatedEvent(
        String correlationId, // 👈 搶票事務 ID
        Long orderId,
        Integer userId,
        LocalDateTime createdAt,
        List<OrderAddRequest> items
) {
    public record SeatLockRequest(
            String orderId,       // 訂單明細 ID
            Integer userId,       // 使用者 ID
            Long orderDateTime,   // 訂單時間戳
            String eventsId,      // 活動 ID
            String orderArea,     // 購買區域
            Integer qty,          // 購買數量
            String action         // "LOCK" 或 "RELEASE"
    ) {}
}
