package com.concer.backend.area.Service;

import com.concer.backend.Request.OrderAddRequest;
import com.concer.backend.area.DAO.AreaRepository;
import com.concer.backend.area.Entity.Area;
import com.concer.backend.events.Entity.Events;
import com.concer.backend.orders.Entity.Orders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AreaServiceImpl implements AreaService {
    @Autowired
    private AreaRepository areaRepository;

    @Override
    public void insert(List<Area> areas) {
        for (Area a : areas) {
            areaRepository.save(a);
        }
    }

    @Override
    public Optional<Boolean> checkQty(List<OrderAddRequest> req) {
        for(OrderAddRequest data:req){
            Events events = new Events();
            events.setEventsId(data.getEventsId());
            Area area = areaRepository.findByEventsIdAndAreaName(events, data.getOrderArea());
            //使用save方法進行跟新， 會把全部欄位都更新過(效率較不好)
            if(area.getQty() < Integer.parseInt(data.getOrderQty())){
                return Optional.of(false);
            }
        }
        return Optional.of(true);
    }

    @Override
    public void updateQty(Orders orders) {
        Events events = new Events();
        events.setEventsId(orders.getEventsId());

        Area area = areaRepository.findByEventsIdAndAreaName(events, orders.getOrederArea());
        //使用save方法進行跟新， 會把全部欄位都更新過(效率較不好)

        area.setQty(area.getQty() - orders.getOrderQty());
        areaRepository.save(area);
    }



    @Override
    public void refundQty(List<Orders> orders) {
        for (Orders data : orders) {
            Events events = new Events();
            events.setEventsId(data.getEventsId());
            areaRepository.refundQty(events,
                    data.getOrederArea(), data.getOrderQty());
        }
    }
}
