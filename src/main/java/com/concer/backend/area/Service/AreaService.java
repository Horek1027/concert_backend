package com.concer.backend.area.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.concer.backend.Request.OrderAddRequest;
import com.concer.backend.area.DAO.AreaRepository;
import com.concer.backend.area.Entity.Area;
import com.concer.backend.area.MyBatisPlus.MyBatisPlusAreaEntity;
import com.concer.backend.orders.Entity.Orders;
import com.concer.backend.orders.MyBatisPlus.MyBatisPlusOrdersEntity;
import com.concer.backend.users.DAO.UserRepository;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;

public interface AreaService  extends IService<MyBatisPlusAreaEntity> {

 void insert(List<MyBatisPlusAreaEntity> areas);

 boolean checkAndUpdateQty (List<MyBatisPlusOrdersEntity> orders);

 boolean refundQty(List<MyBatisPlusOrdersEntity> orders);

 boolean checkQty(List<OrderAddRequest> req);
}
