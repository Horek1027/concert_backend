package com.concer.backend.events.Service;

import com.concer.backend.Request.EventsRequest;

import com.concer.backend.Response.EventsResponse;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.area.Entity.Area;
import com.concer.backend.events.DAO.EventsRepository;
import com.concer.backend.events.Entity.Events;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EventsServiceImpl implements EventsService {
    @Autowired
    private EventsRepository eventsRepository;

    @Override
    public RestfulResponse<Iterable<Events>> getAllEvents() {
      List<Events> list = eventsRepository.findAll();

      RestfulResponse<Iterable<Events>> response =new RestfulResponse<>("0000","搜尋到全部資料",list);
      return  response;
    }

    //單一搜尋
    @Override
    public Optional<Events> getEventsInfo(Integer eventsId) {

        return eventsRepository.findById(eventsId);
    }

    //關鍵字搜尋
    @Override
    public RestfulResponse<List<Events>> wordSerchEvent(String input) {
        System.out.println("前端送來的搜尋字串:" + input);

        List<Events> eventsList = eventsRepository.searchProgramInfoByName(input);
        if(eventsList.isEmpty()){
            RestfulResponse<List<Events>> response = new RestfulResponse<>("-0001", "關鍵字查無資料", eventsList);
            return response;

        }
        RestfulResponse<List<Events>> response = new RestfulResponse<>("0000", "關鍵字搜尋成功", eventsList);
        return response;
    }

    @Override
    public RestfulResponse<String> insert(EventsRequest req) {
        if (req != null) {
            try {
                Events events = new Events();
                events.setUserId(req.getUserId());
                events.setEvnetsName(req.getEvnetsName());
                events.setEventsDetails(req.getEventsDetails());
                events.setEventsLocation(req.getEventsLocation());
                events.setEventsOrganizer(req.getEventsOrganizer());
                events.setEventDate(req.getEventDate());
                events.setShelfTime(req.getShelfTime());
                events.setOffSaleTime(req.getOffSaleTime());
                events.setImage1(req.getImage1());

                //直接在Events方法裡面建立List<Area>的處理
                List<Area> areas = new ArrayList<>();
                for (Area data : req.getAreas()) {
                    Area area = new Area();
                    area.setAreaName(data.getAreaName());
                    area.setAreaPrice(data.getAreaPrice());
                    area.setQty(data.getQty());
                    area.setEventsId(events);
                    areas.add(area);
                }
                //上面設定完的list放入events實體
                events.setArea(areas);

                eventsRepository.save(events);
                System.out.println("eventsRepository已執行save");

                RestfulResponse<String> response = new RestfulResponse<>("0000", "活動新增成功", "活動新增成功");
                return response;

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            RestfulResponse<String> responseNull =
                    new RestfulResponse<>("-0002", "沒有收到活動參數", "沒有收到活動參數");
            return responseNull;
        }
        RestfulResponse<String> responseFail =
                new RestfulResponse<>("-0001", "活動新增失敗", "活動新增失敗");
        return responseFail;
    }
}
