package com.concer.backend.orders.Service;

import com.concer.backend.Request.FindUserByAccountRequst;
import com.concer.backend.Request.OrderAddRequest;
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

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService{
    private UserRepository userRepository;
    private OrdersRepository ordersRepository;
    private AreaService areaService;
    private EventsRepository eventsRepository;
    @Autowired
    public OrderServiceImpl(UserRepository userRepository , OrdersRepository ordersRepository
                            ,AreaService areaService,EventsRepository eventsRepository){
        this.ordersRepository =ordersRepository;
        this.userRepository =userRepository;
        this.areaService =areaService;
        this.eventsRepository=eventsRepository;
    }

    public RestfulResponse<String> insert(List<OrderAddRequest> req) {

        if(req != null && !req.isEmpty()) {
            for (OrderAddRequest data : req) {
                System.out.println("Received request: " + data.toString());
                Users users = userRepository.findByAccount(data.getAccount());
                if(users != null){
                    Orders order = new Orders();
                    order.setUserId(users.getUserId());
                    order.setEventsId(data.getEventsId());
                    order.setOrederArea(data.getOrderArea());
                    order.setOrderQty(Integer.valueOf(data.getOrderQty()));
                    order.setOrderPrice(data.getOrderPrice());
                    order.setOrderDate(new Date());
                    order.setOrder_status(0);
                    System.out.println("裝箱完的Order資料:" +order);
                    ordersRepository.save(order);
                    areaService.updateQty(order);//訂單成立後要減少座位數量
                }else{
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
    public RestfulResponse<List<OrdersResponse>> getUserOrders(FindUserByAccountRequst req) {
        System.out.println("傳入的accout: " +req);
        Users users = userRepository.findByAccount(req.getAccount());
        System.out.println("找到的user資料:" +users);

        List<Orders> orders =  ordersRepository.findByUserId(users.getUserId());

        if (!orders.isEmpty()){
            List<OrdersResponse> responseList = orders.stream().map(order->{

                OrdersResponse response = new OrdersResponse(
                        order.getOrderId(), order.getEventsId(), order.getUserId(),
                        order.getOrederArea(), order.getOrderQty(), order.getOrderPrice(),
                        order.getOrderDate(), order.getOrder_status());
                return response;
            }).collect(Collectors.toList());

            RestfulResponse<List<OrdersResponse>> response =new RestfulResponse<>(
                    "0000","搜尋成功",responseList);
            System.out.println("收尋到的會員訂單資料: " +responseList);
            return  response;
        }
        RestfulResponse<List<OrdersResponse>> responseFail =new RestfulResponse<>(
                "-0001","搜尋失敗",null);
        return  responseFail;
    }
}
