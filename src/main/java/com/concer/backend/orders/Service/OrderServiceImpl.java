package com.concer.backend.orders.Service;

import com.concer.backend.Request.FindUserByAccountRequst;
import com.concer.backend.Request.OrderAddRequest;
import com.concer.backend.Request.OrderCancelRequest;
import com.concer.backend.Response.OrderMergeData;
import com.concer.backend.Response.OrdersResponse;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.area.DAO.AreaRepository;
import com.concer.backend.area.Service.AreaService;
import com.concer.backend.events.DAO.EventsRepository;
import com.concer.backend.events.Entity.Events;
import com.concer.backend.orders.DAO.OrdersRepository;
import com.concer.backend.orders.Entity.Orders;
import com.concer.backend.users.DAO.UserRepository;
import com.concer.backend.users.Entity.Users;
import com.concer.backend.users.Service.UsersService;
import org.hibernate.query.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
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

    public RestfulResponse<String> insert(List<OrderAddRequest> req) {
        if (req != null && !req.isEmpty()) {
            for (OrderAddRequest data : req) {
                System.out.println("Received request: " + data.toString());
                Users users = userRepository.findByAccount(data.getAccount());
                if (users != null) {
                    Orders order = new Orders();
                    order.setUserId(users.getUserId());
                    order.setEventsId(data.getEventsId());
                    order.setOrederArea(data.getOrderArea());
                    order.setOrderQty(Integer.valueOf(data.getOrderQty()));
                    order.setOrderPrice(data.getOrderPrice());
                    order.setOrderDate(new Date());
                    order.setOrderStatus(0);
                    System.out.println("裝箱完的Order資料:" + order);
                    ordersRepository.save(order);
                    areaService.updateQty(order);//訂單成立後要減少座位數量
                } else {
                    System.out.println("找不到user資料");
                }
            }
            RestfulResponse<String> response = new RestfulResponse<>("0000", "成功", "接收到後端的資料");
            return response;
        } else {
            RestfulResponse<String> responseFail = new RestfulResponse<>("-0001", "失敗", "後端收到不正確資料");
            return responseFail;
        }
    }

    @Override
    public RestfulResponse<List<OrderMergeData>> getUserOrders(FindUserByAccountRequst req) {
        System.out.println("傳入的accout: " + req);
        Users users = userRepository.findByAccount(req.getAccount());
        System.out.println("找到的user資料:" + users);

        List<Orders> orders = ordersRepository.findByUserId(users.getUserId());

        if (!orders.isEmpty()) {
            Map<Integer, List<Orders>> ordersByEventsId = orders.stream()
                    .collect(Collectors.groupingBy(Orders::getEventsId));
            List<OrderMergeData> mergeDataList = new ArrayList<>();

            for (Map.Entry<Integer, List<Orders>> entry : ordersByEventsId.entrySet()) {
                Integer eventsId = entry.getKey();

                List<Orders> ordersForEventsId = entry.getValue(); //把oders的value放入該List

                // 創建 OrderMergeData 對象並設置基本屬性
                OrderMergeData mergeData = new OrderMergeData();
                mergeData.setUserId(users.getUserId());
                mergeData.setEventsId(eventsId);

                int totalQty = 0;
                int totalAmount = 0;
                for (Orders order : ordersForEventsId) {
                    totalQty += order.getOrderQty();
                    totalAmount += (order.getOrderQty() * order.getOrderPrice());
                }
                mergeData.setTotalQty(totalQty);
                mergeData.setTotalAmount(totalAmount);
                mergeData.setCreateDate(ordersForEventsId.get(0).getOrderDate().toString());
                mergeData.setStatus(ordersForEventsId.get(0).getOrderStatus());

                // 將訂單數據添加到 detail 中
                List<OrdersResponse> orderList = ordersForEventsId.stream().map(order -> {
                    Events event = eventsRepository.findById(order.getEventsId()).orElse(null);
                    String eventName = (event != null) ? event.getEvnetsName() : "";
                    String evnetDate = (event != null) ? event.getEventDate() : "";
                    return new OrdersResponse(
                            order.getOrderId(), order.getEventsId(), order.getUserId(),
                            order.getOrederArea(), order.getOrderQty(), order.getOrderPrice(),
                            order.getOrderDate(), order.getOrderStatus(), eventName,evnetDate);
                }).collect(Collectors.toList());

                mergeData.setEventsDate(orderList.isEmpty() ? "" : orderList.get(0).getEventsDate());
                mergeData.setEventsName(orderList.isEmpty() ? "" : orderList.get(0).getEventsName());
                mergeData.setDetail(orderList);
                // 將合併後的 OrderMergeData 對象添加到 mergeDataList 中
                mergeDataList.add(mergeData);
            }

            RestfulResponse<List<OrderMergeData>> response = new RestfulResponse<>(
                    "0000", "搜尋成功", mergeDataList);
            System.out.println("收尋到的會員訂單資料: " + mergeDataList);
            return response;
        }

        RestfulResponse<List<OrderMergeData>> responseFail = new RestfulResponse<>(
                "-0001", "搜尋失敗", null);
        return responseFail;
    }

    @Override
    public RestfulResponse<String> cancelOrders(OrderCancelRequest req) {

        if (req != null){
            ordersRepository.updateStatus(req.getUserId(), req.getEventsId());

            List<Orders> orders =  ordersRepository.
                    findByUserIdAndEventsId(req.getUserId(), req.getEventsId());
            areaService.refundQty(orders);
            RestfulResponse<String> response = new RestfulResponse<>
                    ("0000","成功","取消成功");
            return  response;
        }

        RestfulResponse<String> responseFail = new RestfulResponse<>
                ("-0001","失敗","取消失敗");
        return responseFail;
    }
}








//OrderMergeData mergeData = new OrderMergeData();
//            mergeData.setUserId(users.getUserId());
//        mergeData.setEventsId(orders.get(0).getEventsId());
//            Integer  totalQty =0;
//            Integer totalAmount =0;
//            for (OrdersResponse  data : orderList){
//                totalQty += data.getOrderQty();
//                totalAmount+= (data.getOrderQty() * data.getOrderPrice());
//            }
//            mergeData.setTotalQty(totalQty);
//            mergeData.setTotalAmount(totalAmount);
//            mergeData.setCreateDate(orders.get(0).getOrderDate().toString());
//            mergeData.setStatus(orders.get(0).getOrder_status());
//            mergeData.setDetail(orderList);