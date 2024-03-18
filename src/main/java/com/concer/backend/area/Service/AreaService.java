package com.concer.backend.area.Service;

import com.concer.backend.area.DAO.AreaRepository;
import com.concer.backend.area.Entity.Area;
import com.concer.backend.orders.Entity.Orders;
import com.concer.backend.users.DAO.UserRepository;

import javax.swing.text.html.Option;
import java.util.List;

public interface AreaService {

 void insert(List<Area> areas);

 void updateQty (Orders orders);

 void refundQty(List<Orders> orders);

}
