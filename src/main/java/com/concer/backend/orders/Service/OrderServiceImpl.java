package com.concer.backend.orders.Service;

import com.concer.backend.Request.FindUserByAccountRequst;
import com.concer.backend.Request.OrderAddRequest;
import com.concer.backend.Request.OrderCancelRequest;
import com.concer.backend.Response.OrderMergeData;
import com.concer.backend.Response.OrdersResponse;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.area.Service.AreaService;
import com.concer.backend.events.DAO.EventsRepository;
import com.concer.backend.events.Entity.Events;
import com.concer.backend.orders.DAO.OrdersRepository;
import com.concer.backend.orders.Entity.Orders;
import com.concer.backend.users.DAO.UserRepository;
import com.concer.backend.users.Entity.Users;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {
    private UserRepository userRepository;
    private OrdersRepository ordersRepository;
    private AreaService areaService;
    private EventsRepository eventsRepository;

    @Autowired
    public OrderServiceImpl(UserRepository userRepository, OrdersRepository ordersRepository
            , AreaService areaService, EventsRepository eventsRepository) {
        this.ordersRepository = ordersRepository;
        this.userRepository = userRepository;
        this.areaService = areaService;
        this.eventsRepository = eventsRepository;
    }
    @Override
    @Transactional
    public RestfulResponse<String> insert(List<OrderAddRequest> req) {
        Date orderDate = new Date();
        if (req == null || req.isEmpty()) {
            return new RestfulResponse<>("-0001", "失敗", "req是空值");
        }

        // 1. 一次查出 User (避免迴圈內重複查詢)
        Users users = userRepository.findByAccount(req.get(0).getAccount());
        if (users == null) return new RestfulResponse<>("-0001", "失敗", "用戶不存在");

        // 2. 先進行庫存檢查 (請確保 checkQty 已經改為批量檢查)
        if (!areaService.checkQty(req)) {
            return new RestfulResponse<>("-0002", "失敗", "有座位數量不足");
        }

        // 3. 準備訂單資料
        List<Orders> orderList = new ArrayList<>();
        for (OrderAddRequest data : req) {
            if (data.getOrderQty() == null || "0".equals(data.getOrderQty())) {
                continue;
            }
            Orders order = new Orders();
            order.setUserId(users.getUserId());
            order.setEventsId(data.getEventsId());
            order.setOrderArea(data.getOrderArea());
            order.setOrderQty(Integer.valueOf(data.getOrderQty()));
            order.setOrderPrice(data.getOrderPrice());
            order.setOrderDate(orderDate);
            order.setOrderStatus(0);
            orderList.add(order);
        }

        // 4. 批次處理
        try {
            // 批次扣庫存
            areaService.updateQty(orderList);
            // 批次插入訂單 (Spring Data JPA 的 saveAll 配合 batch_size 設定) PS:暫時不設定
            ordersRepository.saveAll(orderList);
        } catch (Exception e) {
            throw new RuntimeException("資料庫更新失敗:" , e); // 觸發 Transactional 回滾
        }

        return new RestfulResponse<>("0000", "成功", "訂單建立完成");
    }





//    public RestfulResponse<String> insert(List<OrderAddRequest> req) {
//        if (req.isEmpty()) {
//            return new RestfulResponse<>("-0001", "失敗", "req是空值");
//        }
//
//        if (!areaService.checkQty(req)) {
//            return new RestfulResponse<>("-0002", "失敗", "有座位數量不足");
//        }
//
//        Users users = userRepository.findByAccount(req.get(0).getAccount());
//
//        for (OrderAddRequest data : req) {
//            System.out.println("Received request: " + data.toString());
//            if (users != null) {
//                Orders order = new Orders();
//                order.setUserId(users.getUserId());
//                order.setEventsId(data.getEventsId());
//                order.setOrderArea(data.getOrderArea());
//                order.setOrderQty(Integer.valueOf(data.getOrderQty()));
//                order.setOrderPrice(data.getOrderPrice());
//                order.setOrderDate(new Date());
//                order.setOrderStatus(0);
//                System.out.println("裝箱完的Order資料:" + order);
//
//                try {
//                    areaService.updateQty(order);//減少 areaQty
//                    ordersRepository.save(order);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    return new RestfulResponse<>("-0001", "失敗", "插入table失敗");
//                }
//
//            } else {
//                System.out.println("找不到user資料");
//            }
//
//
//            return new RestfulResponse<>("0000", "成功", "接收到後端的資料");
//        }
//
//        return new RestfulResponse<>("-0001", "失敗", "後端收到不正確資料");
//    }




    @Override
    @Transactional(readOnly = true) // 優化：查詢操作加上 readOnly，效能更好
    public RestfulResponse<List<OrderMergeData>> getUserOrders(FindUserByAccountRequst req) {
        Users users = userRepository.findByAccount(req.getAccount());
        if (users == null) {
            return new RestfulResponse<>("-0001", "查無此會員", null);
        }

        List<Orders> orders = ordersRepository.findByUserId(users.getUserId());
        if (orders.isEmpty()) {
            return new RestfulResponse<>("-0001", "無訂單資料", null);
        }

        // 1. 取得所有不重複的 EventsId
        List<Integer> eventIds = orders.stream()
                .map(Orders::getEventsId)
                .distinct()
                .collect(Collectors.toList());

        // 2. 一次查出所有相關的 Events 並存入 Map
        Map<Integer, Events> eventMap = eventsRepository.findAllById(eventIds)
                .stream()
                .collect(Collectors.toMap(Events::getEventsId, e -> e));
        // --- 【優化整合結束】---


        //相同訂單編號跟建立時間是一筆資料
        Map<String, List<Orders>> ordersByOrder = orders.stream()
                .collect(Collectors.groupingBy(order ->
                        order.getEventsId() + "_" + order.getOrderDate().getTime()
                ));

        List<OrderMergeData> mergeDataList = new ArrayList<>();

        for (Map.Entry<String, List<Orders>> entry : ordersByOrder.entrySet()) {
            List<Orders> ordersForOneOrder = entry.getValue();
            Integer eventsId = ordersForOneOrder.get(0).getEventsId();

            OrderMergeData mergeData = new OrderMergeData();
            mergeData.setUserId(users.getUserId());
            mergeData.setEventsId(eventsId);

            int totalQty = 0;
            int totalAmount = 0;
            for (Orders order : ordersForOneOrder) {
                totalQty += order.getOrderQty();
                totalAmount += order.getOrderQty() * order.getOrderPrice();
            }

            mergeData.setTotalQty(totalQty);
            mergeData.setTotalAmount(totalAmount);
            mergeData.setCreateDate(ordersForOneOrder.get(0).getOrderDate().toString());
            mergeData.setStatus(ordersForOneOrder.get(0).getOrderStatus());

            List<OrdersResponse> orderList = ordersForOneOrder.stream().map(order -> {
                Events event = eventMap.get(order.getEventsId());
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
    @Override
    public RestfulResponse<String> cancelOrders(OrderCancelRequest req) {



        log.info("收到的資料:" + req);



        if (req != null) {
            int count = ordersRepository.updateStatusByOrderIds(req.getOrdersId());
            System.out.println("更新筆數 = " + count);

            List<Orders> orders = ordersRepository.
                    findByUserIdAndEventsId(req.getOrdersId());
            areaService.refundQty(orders);
            return new RestfulResponse<>
                    ("0000", "成功", "取消成功");
        }
        return new RestfulResponse<>
                ("-0001", "失敗", "取消失敗");
    }
}
