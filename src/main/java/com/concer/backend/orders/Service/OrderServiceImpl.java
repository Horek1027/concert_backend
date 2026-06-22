package com.concer.backend.orders.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.concer.backend.Request.FindUserByAccountRequst;
import com.concer.backend.Request.OrderAddRequest;
import com.concer.backend.Request.OrderCancelRequest;
import com.concer.backend.Response.OrderMergeData;
import com.concer.backend.Response.OrdersResponse;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.area.Service.AreaService;
import com.concer.backend.events.DAO.EventsRepository;
import com.concer.backend.events.MyBatisPlus.MyBatisPlusEventsEntity;
import com.concer.backend.events.MyBatisPlus.MyBatisPlusEventsMapper;
import com.concer.backend.kafka.DTO.ReserveRequest;
import com.concer.backend.kafka.Event.OrderCreatedEvent;
import com.concer.backend.kafka.KafkaTopics;
import com.concer.backend.kafka.OrderKafkaProducer;
import com.concer.backend.orders.Entity.OrderStatus;
import com.concer.backend.orders.MyBatisPlus.MyBatisPlusOrdersEntity;
import com.concer.backend.orders.MyBatisPlus.MyBatisPlusOrdersMapper;
import com.concer.backend.users.MyBatisPlus.MyBatisPlusUsersEntity;
import com.concer.backend.users.MyBatisPlus.MyBatisPlusUsersMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl
        extends ServiceImpl<MyBatisPlusOrdersMapper, MyBatisPlusOrdersEntity>
        implements OrderService {

    private final AreaService areaService;
    //private final UserRepository userRepository;
//    private final OrdersRepository ordersRepository;
    private final EventsRepository eventsRepository;
    private final MyBatisPlusUsersMapper myBatisPlusUsersMapper;
    private final MyBatisPlusOrdersMapper myBatisPlusOrdersMapper;
    private final MyBatisPlusEventsMapper myBatisPlusEventsMapper;
    private final OrderKafkaProducer orderKafkaProducer;

    // 加上這一行來注入 KafkaTemplate

    private final KafkaTemplate<String, Object> kafkaTemplate;

//    @Override
//    @Transactional
//    public RestfulResponse<String> insert(List<OrderAddRequest> req) {
//        Date orderDate = new Date();
//        if (req == null || req.isEmpty()) {
//            return new RestfulResponse<>("-0001", "失敗", "req是空值");
//        }
//
//        // 1. 一次查出 User (避免迴圈內重複查詢)
////        Users users = userRepository.findByAccount(req.get(0).getAccount());
//        MyBatisPlusUsersEntity users = myBatisPlusUsersMapper.selectOne(
//                new LambdaQueryWrapper<MyBatisPlusUsersEntity>()
//                        .eq(MyBatisPlusUsersEntity::getAccount, req.get(0).getAccount())
//        );
//        if (users == null) return new RestfulResponse<>("-0001", "失敗", "用戶不存在");
//
//
//        // 2. 準備訂單資料
//        List<MyBatisPlusOrdersEntity> orderList = new ArrayList<>();
//        for (OrderAddRequest data : req) {
//            if (data.getOrderQty() == null || "0".equals(data.getOrderQty())) {
//                continue;
//            }
//            MyBatisPlusOrdersEntity order = new MyBatisPlusOrdersEntity();
//            order.setUserId(users.getUserId());
//            order.setEventsId(data.getEventsId());
//            order.setOrderArea(data.getOrderArea());
//            order.setOrderQty(Integer.valueOf(data.getOrderQty()));
//            order.setOrderPrice(data.getOrderPrice());
//            order.setOrderDate(orderDate);
//            order.setOrderStatus(0);
//            orderList.add(order);
//        }
//        // 3.扣座位跟檢查座位
//        if (!areaService.checkAndUpdateQty(orderList)) {
//            log.error("訂單建立失敗，原因: 有座位數量不足或系統異常");
//            //TransactionAspectSupport 在程式裡手動告訴 Spring：「目前這個 @Transactional 交易最後不可以 commit，要 rollback。」
//            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
//            return new RestfulResponse<>("-0002", "失敗", "有座位數量不足");
//        }
//
//
//        // 4.成立訂單
//        try {
//            this.saveBatch(orderList);
//        } catch (Exception e) {
//            throw new RuntimeException("資料庫更新失敗:", e); // 觸發 Transactional 回滾
//        }
//
//        return new RestfulResponse<>("0000", "成功", "訂單建立完成");
//    }

    @Override
    public RestfulResponse<String> insert(List<OrderAddRequest> req) {
        if (req == null || req.isEmpty()) {
            return new RestfulResponse<>("-0001", "失敗", "訂單資料不可為空");
        }

        // 1. 查出使用者（避免迴圈內重複查詢）
        MyBatisPlusUsersEntity users = myBatisPlusUsersMapper.selectOne(
                new LambdaQueryWrapper<MyBatisPlusUsersEntity>()
                        .eq(MyBatisPlusUsersEntity::getAccount, req.get(0).getAccount())
        );
        if (users == null) {
            return new RestfulResponse<>("-0002", "失敗", "找不到使用者");
        }

        // 2. 產生本次搶票事務唯一的 Correlation ID
        String correlationId = java.util.UUID.randomUUID().toString();

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .correlationId(correlationId)
                .items(req)
                .userId(users.getUserId())
                .createdAt(LocalDateTime.now())
                .build();

        // 3. 發送至 ORDER_CREATE，由 OrderKafkaConsumer 異步消費並呼叫 insertFromKafka
        //    Key = eventsId_orderArea，確保同區域訂單進入同個 Partition
        String kafkaKey = req.get(0).getEventsId() + "_" + req.get(0).getOrderArea();
        orderKafkaProducer.sendOrderCreateMessage(kafkaKey, event);

        // 4. 立即回傳 correlationId，供前端輪詢搶票結果
        return new RestfulResponse<>("0000", "搶票處理中", correlationId);
    }
    private final ObjectMapper objectMapper = new ObjectMapper();
//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public void insertFromKafka(OrderCreatedEvent event) throws JsonProcessingException {
//        Date orderDate = new Date();
//
//        // 1. 將訂單明細寫入資料庫，狀態預設為 0 (PROCESSING / 處理中)
//        //    此時不做任何庫存扣減，庫存判斷交由 Kafka Streams (RocksDB) 負責
//        List<MyBatisPlusOrdersEntity> orderList = new ArrayList<>();
//        for (OrderAddRequest data : event.items()) {
//            if (data.getOrderQty() == null || "0".equals(data.getOrderQty())) {
//                continue;
//            }
//            MyBatisPlusOrdersEntity order = new MyBatisPlusOrdersEntity();
//            order.setUserId(event.userId());
//            order.setEventsId(data.getEventsId());
//            order.setOrderArea(data.getOrderArea());
//            order.setOrderQty(Integer.valueOf(data.getOrderQty()));
//            order.setOrderPrice(data.getOrderPrice());
//            order.setOrderDate(orderDate);
//            order.setOrderStatus(OrderStatus.PROCESSING.getCode()); // 初始狀態為 0
//            orderList.add(order);
//        }
//
//        if (orderList.isEmpty()) {
//            log.warn("收到的 Kafka 訂票事件無有效品項，略過。userId: {}", event.userId());
//            return;
//        }
//
//        try {
//            // 2. 批次寫入資料庫，取得自增的 orderId
//            this.saveBatch(orderList);
//            log.info("【訂單建立】PENDING 訂單已寫入 DB，共 {} 筆。userId: {}, correlationId: {}",
//                    orderList.size(), event.userId(), event.correlationId());
//
//            // 3. 封裝要發送至 Kafka 的任務清單（先收集，等一下 Commit 成功後才發）
//            List<KafkaSendTask> kafkaTasks = new ArrayList<>();
//            for (MyBatisPlusOrdersEntity order : orderList) {
//                com.concer.backend.kafka.streams.ReservationStreamProcessor.StreamRequest payload =
//                        new com.concer.backend.kafka.streams.ReservationStreamProcessor.StreamRequest();
//                payload.orderId = String.valueOf(order.getOrderId());
//                payload.qty = order.getOrderQty();
//                payload.action = "LOCK";
//
//                String kafkaKey = order.getEventsId() + "_" + order.getOrderArea();
////                kafkaTasks.add(new KafkaSendTask(kafkaKey, payload));
//                // 👈 核心修正 1：手動將物件轉為 JSON 字串
//                String jsonStringPayload = objectMapper.writeValueAsString(payload);
//
//                // 傳入的是 String，而不是物件
//                kafkaTasks.add(new KafkaSendTask(kafkaKey, jsonStringPayload));
//            }
//
//            // 4. ⭐ 核心優化：註冊監聽器，當且僅當資料庫事務 Commit 成功後，才真正非同步發送 Kafka 訊息
//            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//                @Override
//                public void afterCommit() {
//                    for (KafkaSendTask task : kafkaTasks) {
//                        // 已修正：補上第三個參數 task.getPayload()，不再是空包彈
//                        kafkaTemplate.send(KafkaTopics.RESERVE_REQUEST, task.getKey(), task.getPayload());
//                    }
//                    log.info("【KStreams 請求發送】DB 提交成功，已成功將 {} 筆搶票請求發送至 KStreams。", kafkaTasks.size());
//                }
//            });
//
//        } catch (Exception e) {
//            log.error("【insertFromKafka 異常】寫入 DB 失敗，觸發回滾。", e);
//            throw e;
//        }
//    }
//
//    // 輔助類別：用來暫存準備送往 Kafka 的 Key 與 Payload
//    private static class KafkaSendTask {
//        private final String key;
//        private final String payload; // 這裡改為 String
//        public KafkaSendTask(String key, String payload) {
//            this.key = key;
//            this.payload = payload;
//        }
//
//        public String getKey() { return key; }
//        public String getPayload() { return payload; }
//    }

////不使用自動factory的版本
//@Override
//@Transactional(rollbackFor = Exception.class)
//public void insertFromKafka(OrderCreatedEvent event) {
//    Date orderDate = new Date();
//
//    // 1. 將訂單明細寫入資料庫，狀態預設為 0 (PROCESSING / 處理中)
//    List<MyBatisPlusOrdersEntity> orderList = new ArrayList<>();
//    for (OrderAddRequest data : event.items()) {
//        if (data.getOrderQty() == null || "0".equals(data.getOrderQty())) {
//            continue;
//        }
//        MyBatisPlusOrdersEntity order = new MyBatisPlusOrdersEntity();
//        order.setUserId(event.userId());
//        order.setEventsId(data.getEventsId());
//        order.setOrderArea(data.getOrderArea());
//        order.setOrderQty(Integer.valueOf(data.getOrderQty()));
//        order.setOrderPrice(data.getOrderPrice());
//        order.setOrderDate(orderDate);
//        order.setOrderStatus(OrderStatus.PROCESSING.getCode()); // 初始狀態為 0
//        orderList.add(order);
//    }
//
//    if (orderList.isEmpty()) {
//        log.warn("收到的 Kafka 訂票事件無有效品項，略過。userId: {}", event.userId());
//        return;
//    }
//
//    try {
//        // 2. 批次寫入資料庫，取得自增的 orderId
//        this.saveBatch(orderList);
//        log.info("【訂單建立】PENDING 訂單已寫入 DB，共 {} 筆。userId: {}, correlationId: {}",
//                orderList.size(), event.userId(), event.correlationId());
//
//        // 3. 封裝要發送至 Kafka 的任務清單（暫存實體物件，等 Commit 成功後才發）
//        List<KafkaSendTask> kafkaTasks = new ArrayList<>();
//        int totalSegments = orderList.size();
//
//        for (MyBatisPlusOrdersEntity order : orderList) {
//
//            // 💡 修正點 1：改用新抽出來的獨立 DTO 物件
//            ReserveRequest payload = new ReserveRequest();
//            payload.setOrderId(String.valueOf(order.getOrderId()));
//            payload.setQty(order.getOrderQty());
//            payload.setAction("LOCK");
//
//            // 核心設定這次訂單有幾個位子
//            payload.setTotalSegments(totalSegments);
//            String kafkaKey = order.getEventsId() + "_" + order.getOrderArea();
//
//            // 💡 修正點 2：直接塞入物件，不再呼叫 objectMapper 轉字串！
//            kafkaTasks.add(new KafkaSendTask(kafkaKey, payload));
//        }
//
//        // 4. ⭐ 核心優化：事務 Commit 成功後，才真正發送 Kafka 訊息
//        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//            //當 SQLDB 順利寫入後，才真正將狀態為 LOCK 的 ReserveRequest 拋向 KafkaTopics.RESERVE_REQUEST
//            @Override
//            public void afterCommit() {
//                for (KafkaSendTask task : kafkaTasks) {
//                    // 修正點 3：此時透過 KafkaTemplate 發送出去的是純物件，
//                    // Spring 依照 properties 的全域設定，會自動在底層幫你完美序列化「一次」！
//                    //因為kafka 是非同步即使 跑for 迴圈也沒有N+1 問題
//                    kafkaTemplate.send(KafkaTopics.RESERVE_REQUEST, task.getKey(), task.getPayload());
//                }
//                log.info("【KStreams 請求發送】DB 提交成功，已成功將 {} 筆搶票請求發送至 KStreams。", kafkaTasks.size());
//            }
//        });
//    } catch (Exception e) {
//        log.error("【insertFromKafka 異常】寫入 DB 失敗，觸發回滾。", e);
//        throw e;
//    }
//}
//
//    /**
//     * 💡 修正點 4：調整輔助類別，將 payload 的型態由 String 改為封裝物件 ReserveRequest
//     */
//    @Getter
//    private static class KafkaSendTask {
//        private final String key;
//        private final ReserveRequest payload; // 👈 這裡改為 DTO 物件
//
//        public KafkaSendTask(String key, ReserveRequest payload) {
//            this.key = key;
//            this.payload = payload;
//        }
//
//    }
private final SecureRandom secureRandom = new SecureRandom();
    

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertFromKafka(OrderCreatedEvent event) {
        Date orderDate = new Date();
        SecureRandom secureRandom = new SecureRandom();
        // 使用正整數的亂數。
        // 或者使用 secureRandom.nextInt(Integer.MAX_VALUE) 確保一定是正數
        // 但若要徹底防重，分散式系統更建議使用 UUID.randomUUID().toString()
        int randomValue = secureRandom.nextInt(Integer.MAX_VALUE);

        // 範圍變成 1 ~ 2,147,483,647 (剛好包含 MAX_VALUE)
//        int randomValue = 1 + secureRandom.nextInt(Integer.MAX_VALUE);

        // 產生一個介於 1 到 9999 之間（包含 1000，不包含 10000）的 4 位數亂數
        // int fourDigits = secureRandom.ints(1, 10000).findFirst().getAsInt();
        // 1. 將訂單明細寫入資料庫，狀態預設為 PROCESSING（處理中）
        List<MyBatisPlusOrdersEntity> orderList = new ArrayList<>();
        for (OrderAddRequest data : event.items()) {
            if (data.getOrderQty() == null || "0".equals(data.getOrderQty())) {
                continue;
            }
            MyBatisPlusOrdersEntity order = new MyBatisPlusOrdersEntity();
            order.setOrderId(randomValue);
            order.setUserId(event.userId());
            order.setEventsId(data.getEventsId());
            order.setOrderArea(data.getOrderArea());
            order.setOrderQty(Integer.valueOf(data.getOrderQty()));
            order.setOrderPrice(data.getOrderPrice());
            order.setOrderDate(orderDate);
            order.setOrderStatus(OrderStatus.PROCESSING.getCode());
            orderList.add(order);
        }
        if (orderList.isEmpty()) {
            log.warn("收到的 Kafka 訂票事件無有效品項，略過。userId: {}", event.userId());
            return;
        }
        try {
            // 2. 批次寫入資料庫，自增 orderId 會在此填入 entity
            this.saveBatch(orderList);
            log.info("【訂單建立】PENDING 訂單已寫入 DB，共 {} 筆。userId: {}, correlationId: {}",
                    orderList.size(), event.userId(), event.correlationId());
            // 3. 封裝 Kafka 發送任務清單（等 DB Commit 成功後才發，避免 DB 回滾但 Kafka 已發出）
            List<KafkaSendTask> kafkaTasks = new ArrayList<>();
            int totalSegments = orderList.size();

            for (MyBatisPlusOrdersEntity order : orderList) {
                ReserveRequest payload = new ReserveRequest();
                payload.setOrderId(String.valueOf(order.getOrderId()));
                payload.setQty(order.getOrderQty());
                payload.setAction("LOCK");
                payload.setTotalSegments(totalSegments);
                // 【修改】新增：同時記錄 eventsId 與 orderArea 到 ReserveRequest
                // 原因：KStream aggregate 完成後，finalStream 的 mapValues 需要從 ReserveResult.details
                // 中取得每筆的 eventsId + orderArea 才能組出正確的 RELEASE key。
                // ReserveResult 是由 ReserveRequest 的欄位建構，所以 ReserveRequest 必須帶齊這兩個欄位
                payload.setEventsId(order.getEventsId());
                payload.setOrderArea(order.getOrderArea());

                String kafkaKey = order.getEventsId() + "_" + order.getOrderArea();
                kafkaTasks.add(new KafkaSendTask(kafkaKey, payload));
            }
            // 4. ⭐ 核心優化：事務 Commit 成功後，才真正發送 Kafka 訊息
            // 原因：若在 @Transactional 內直接 send，DB 之後 rollback 但 Kafka 訊息已發出，
            //  KStream 會對一筆不存在的訂單執行 LOCK，造成幽靈庫存扣減
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (KafkaSendTask task : kafkaTasks) {
                        kafkaTemplate.send(KafkaTopics.RESERVE_REQUEST, task.key(), task.payload());
                    }
                    log.info("【KStreams 請求發送】DB 提交成功，已成功將 {} 筆搶票請求發送至 KStreams。", kafkaTasks.size());
                }
            });
        } catch (Exception e) {
            log.error("【insertFromKafka 異常】寫入 DB 失敗，觸發回滾。", e);
            throw e;
        }
    }

    /**
         * 內部輔助類別，封裝一筆待發送的 Kafka 訊息（key + payload）
         * payload 為 DTO 物件，由 Spring KafkaTemplate 統一序列化，不在這裡手動轉 JSON
         */
        private record KafkaSendTask(String key, ReserveRequest payload) {
    }


    @Override
    @Transactional(readOnly = true) // 優化：查詢操作加上 readOnly，效能更好
    public RestfulResponse<List<OrderMergeData>> getUserOrders(FindUserByAccountRequst req) {
//        Users users = userRepository.findByAccount(req.getAccount());
        MyBatisPlusUsersEntity users = myBatisPlusUsersMapper.selectOne(
                new LambdaQueryWrapper<MyBatisPlusUsersEntity>()
                        .eq(MyBatisPlusUsersEntity::getAccount, req.getAccount())
        );
        if (users == null) {
            return new RestfulResponse<>("-0001", "查無此會員", null);
        }

//        List<Orders> orders = ordersRepository.findByUserId(users.getUserId());
        List<MyBatisPlusOrdersEntity> orders = myBatisPlusOrdersMapper.selectList(
                new LambdaQueryWrapper<MyBatisPlusOrdersEntity>()
                        .eq(MyBatisPlusOrdersEntity::getUserId, users.getUserId())
        );
        if (orders.isEmpty()) {
            return new RestfulResponse<>("-0001", "無訂單資料", null);
        }

        // 1. 取得所有不重複的 EventsId
        List<Integer> eventIds = orders.stream()
                .map(MyBatisPlusOrdersEntity::getEventsId)
                .distinct()
                .collect(Collectors.toList());

        // 2. 一次查出所有相關的 Events 並存入 Map
//        Map<Integer, Events> eventMap = eventsRepository.findAllById(eventIds)
//                .stream()
//                .collect(Collectors.toMap(Events::getEventsId, e -> e));

        Map<Integer, MyBatisPlusEventsEntity> eventMapByMybatisPlus = myBatisPlusEventsMapper.selectList(
                        new LambdaQueryWrapper<MyBatisPlusEventsEntity>()
                                .in(MyBatisPlusEventsEntity::getEventsId, eventIds)
                )
                .stream()
                .collect(Collectors.toMap(
                        MyBatisPlusEventsEntity::getEventsId,
                        e -> e
                ));
        // --- 【優化整合結束】---


        //相同訂單編號跟建立時間是一筆資料
        Map<String, List<MyBatisPlusOrdersEntity>> ordersByOrder = orders.stream()
                .collect(Collectors.groupingBy(order ->
                        order.getEventsId() + "_" + order.getOrderDate().getTime()
                ));

        List<OrderMergeData> mergeDataList = new ArrayList<>();

        for (Map.Entry<String, List<MyBatisPlusOrdersEntity>> entry : ordersByOrder.entrySet()) {
            List<MyBatisPlusOrdersEntity> ordersForOneOrder = entry.getValue();
            Integer eventsId = ordersForOneOrder.get(0).getEventsId();

            OrderMergeData mergeData = new OrderMergeData();
            mergeData.setUserId(users.getUserId());
            mergeData.setEventsId(eventsId);

            int totalQty = 0;
            int totalAmount = 0;
            for (MyBatisPlusOrdersEntity order : ordersForOneOrder) {
                totalQty += order.getOrderQty();
                totalAmount += order.getOrderQty() * order.getOrderPrice();
            }

            mergeData.setTotalQty(totalQty);
            mergeData.setTotalAmount(totalAmount);
            mergeData.setCreateDate(ordersForOneOrder.get(0).getOrderDate().toString());
            mergeData.setStatus(ordersForOneOrder.get(0).getOrderStatus());

            List<OrdersResponse> orderList = ordersForOneOrder.stream().map(order -> {
                MyBatisPlusEventsEntity event = eventMapByMybatisPlus.get(order.getEventsId());
                String eventName = (event != null) ? event.getEventsName() : "";
                String eventDate = (event != null) ? event.getEventDate() : "";

                return new OrdersResponse(
                        order.getOrderId(),
                        order.getEventsId(),
                        order.getUserId(),
                        order.getOrderArea(),
                        order.getOrderQty(),
                        order.getOrderPrice(),
                        order.getOrderDate(),
                        order.getOrderStatus(),
                        eventName,
                        eventDate
                );
            }).collect(Collectors.toList());

            mergeData.setEventsDate(orderList.isEmpty() ? "" : orderList.get(0).getEventsDate());
            mergeData.setEventsName(orderList.isEmpty() ? "" : orderList.get(0).getEventsName());
            mergeData.setDetail(orderList);
            mergeDataList.add(mergeData);
        }

        return new RestfulResponse<>("0000", "搜尋成功", mergeDataList);
    }

    //    @Override
//    public RestfulResponse<List<OrderMergeData>> getUserOrders(FindUserByAccountRequst req) {
//        System.out.println("傳入的accout: " + req);
//        Users users = userRepository.findByAccount(req.getAccount());
//        System.out.println("找到的user資料:" + users);
//
//        List<Orders> orders = ordersRepository.findByUserId(users.getUserId());
//
//        if (!orders.isEmpty()) {
//            Map<Integer, List<Orders>> ordersByEventsId = orders.stream()
//                    .collect(Collectors.groupingBy(Orders::getEventsId));
//            List<OrderMergeData> mergeDataList = new ArrayList<>();
//
//            for (Map.Entry<Integer, List<Orders>> entry : ordersByEventsId.entrySet()) {
//                Integer eventsId = entry.getKey();
//
//                List<Orders> ordersForEventsId = entry.getValue(); //把oders的value放入該List
//
//                // 創建 OrderMergeData 對象並設置基本屬性
//                OrderMergeData mergeData = new OrderMergeData();
//                mergeData.setUserId(users.getUserId());
//                mergeData.setEventsId(eventsId);
//
//                int totalQty = 0;
//                int totalAmount = 0;
//                for (Orders order : ordersForEventsId) {
//                    totalQty += order.getOrderQty();
//                    totalAmount += (order.getOrderQty() * order.getOrderPrice());
//                }
//                mergeData.setTotalQty(totalQty);
//                mergeData.setTotalAmount(totalAmount);
//                mergeData.setCreateDate(ordersForEventsId.get(0).getOrderDate().toString());
//                mergeData.setStatus(ordersForEventsId.get(0).getOrderStatus());
//
//                // 將訂單數據添加到 detail 中
//                List<OrdersResponse> orderList = ordersForEventsId.stream().map(order -> {
//                    Events event = eventsRepository.findById(order.getEventsId()).orElse(null); 這行有N+1
//                    String eventName = (event != null) ? event.getEventsName() : "";
//                    String evnetDate = (event != null) ? event.getEventDate() : "";
//                    return new OrdersResponse(
//                            order.getOrderId(), order.getEventsId(), order.getUserId(),
//                            order.getOrderArea(), order.getOrderQty(), order.getOrderPrice(),
//                            order.getOrderDate(), order.getOrderStatus(), eventName,evnetDate);
//                }).collect(Collectors.toList());
//
//                mergeData.setEventsDate(orderList.isEmpty() ? "" : orderList.get(0).getEventsDate());
//                mergeData.setEventsName(orderList.isEmpty() ? "" : orderList.get(0).getEventsName());
//                mergeData.setDetail(orderList);
//                // 將合併後的 OrderMergeData 對象添加到 mergeDataList 中
//                mergeDataList.add(mergeData);
//            }
//            System.out.println("收尋到的會員訂單資料: " + mergeDataList);
//            return new RestfulResponse<>("0000", "搜尋成功", mergeDataList);
//        }
//        return  new RestfulResponse<>("-0001", "搜尋失敗", null);
//    }
    @Transactional
    @Override
    public RestfulResponse<String> cancelOrders(OrderCancelRequest req) {
            try{
                // log.info("收到的資料:" + req);
                if (req != null) {
//            int count = ordersRepository.updateStatusByOrderIds(req.getOrdersId());
                    int count = myBatisPlusOrdersMapper.update(
                            null,
                            new LambdaUpdateWrapper<MyBatisPlusOrdersEntity>()
                                    .set(MyBatisPlusOrdersEntity::getOrderStatus, OrderStatus.CANCELLED.getCode())
                                    .in(MyBatisPlusOrdersEntity::getOrderId, req.getOrdersId())
                    );
                    System.out.println("更新筆數 = " + count);
//            List<Orders> orders = ordersRepository.findByUserIdAndEventsId(req.getOrdersId());
                    List<MyBatisPlusOrdersEntity> orders = myBatisPlusOrdersMapper.selectList(
                            new LambdaQueryWrapper<MyBatisPlusOrdersEntity>()
                                    .in(MyBatisPlusOrdersEntity::getOrderId, req.getOrdersId())
                    );
                    // 3. 歸還 postgreSQL 庫存
                    if (!areaService.refundQty(orders)) {
                        return new RestfulResponse<>
                                ("-0001", "失敗", "取消失敗");
                    }
                    // 💡 進階安全寫法：確保 DB Commit 成功後才發送 Kafka 訊息給 RocksDB
                    // 4.核心優化：準備發送給 KStream 的 RocksDB 補償訊息
                    // 為了避免重複聚合干擾，比照前面 KStream 的設計，orderId 加上 "RELEASE_" 前綴
                    if (TransactionSynchronizationManager.isActualTransactionActive()) {
                        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                for (MyBatisPlusOrdersEntity order : orders) {
                                    ReserveRequest releaseReq = new ReserveRequest();
                                    releaseReq.setOrderId("RELEASE_" + order.getOrderId());
                                    releaseReq.setEventsId(order.getEventsId());
                                    releaseReq.setOrderArea(order.getOrderArea());
                                    releaseReq.setQty(order.getOrderQty());
                                    releaseReq.setAction("RELEASE");
                                    releaseReq.setTotalSegments(1); // 獨立事件，不需要等待聚合
                                    String kafkaKey = order.getEventsId() + "_" + order.getOrderArea();

                                    // 發送至 Kafka，讓 KStream 去更新 RocksDB
                                    kafkaTemplate.send(KafkaTopics.RESERVE_REQUEST, kafkaKey, releaseReq);
                                    log.info("【訂單取消-發送補償】已發送 RELEASE 訊息至 Kafka. Key: {}, Qty: {}, OrderId: {}",
                                            kafkaKey, order.getOrderQty(), order.getOrderId());
                                }
                                log.info("【訂單取消】DB 提交成功，釋放 RocksDB 庫存訊息已送出");
                            }
                        });
                    } else {
                        // 如果當前沒有 Spring 事務，就直接發送
                        for (MyBatisPlusOrdersEntity order : orders) {
                            ReserveRequest payload = new ReserveRequest();
                            payload.setOrderId(String.valueOf(order.getOrderId()));
                            payload.setEventsId(order.getEventsId());
                            payload.setOrderArea(order.getOrderArea());
                            payload.setQty(order.getOrderQty());
                            payload.setAction("RELEASE");
                            payload.setTotalSegments(1); // 獨立事件，不需要等待聚合
                            String kafkaKey = order.getEventsId() + "_" + order.getOrderArea();
                            // 發送至 Kafka，讓 KStream 去更新 RocksDB
                            kafkaTemplate.send(KafkaTopics.RESERVE_REQUEST, kafkaKey, payload);
                            log.info("【訂單取消-發送補償】已發送 RELEASE 訊息至 Kafka. Key: {}, Qty: {}, OrderId: {}",
                                    kafkaKey, order.getOrderQty(), order.getOrderId());
                        }
                    }
                    return new RestfulResponse<>
                            ("0000", "成功", "取消成功");
                }
            } catch (Exception e) {
                log.error("【❌ 取消訂單異常】", e);
                return new RestfulResponse<>("-0001", "失敗", "系統異常：" + e.getMessage());
            }
        return new RestfulResponse<>
                ("-0001", "失敗", "取消失敗");
    }

}
