package com.concer.backend.events.Service;

import com.concer.backend.Request.*;

import com.concer.backend.Response.EventsResponse;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.area.Entity.Area;
import com.concer.backend.events.DAO.EventsRepository;
import com.concer.backend.events.Entity.Events;
import com.concer.backend.users.DAO.UserRepository;
import com.concer.backend.users.Entity.Users;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Transactional
public class EventsServiceImpl implements EventsService {
    @Autowired
    private EventsRepository eventsRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public RestfulResponse<Iterable<Events>> getAllEvents() {
        List<Events> list = eventsRepository.findAll();

        RestfulResponse<Iterable<Events>> response = new RestfulResponse<>("0000", "搜尋到全部資料", list);
        return response;
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
        if (eventsList.isEmpty()) {
            RestfulResponse<List<Events>> response = new RestfulResponse<>("-0001", "關鍵字查無資料", eventsList);
            return response;

        }
        RestfulResponse<List<Events>> response = new RestfulResponse<>("0000", "關鍵字搜尋成功", eventsList);
        return response;
    }
    //下方Date 日期轉String 日期
    public static Date stringToDate(String dataString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = null;
        try {
            date = sdf.parse(dataString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return date;
    }
    //下方轉換字串日期格式
    public static String formatString(String dateString){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try{
            Date date = sdf.parse(dateString);

//            SimpleDateFormat changeFormat = new SimpleDateFormat("yyyy/M/d（E）HH:mm", Locale.CHINESE);
            SimpleDateFormat changeFormat = new SimpleDateFormat("yyyy/M/d（E）HH:mm");
            changeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Taipei"));

            return changeFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return  null;
        }
    }


    @Override
    public RestfulResponse<String> insert(EventsAndAreaRequest req) {

        System.out.println("接收到的活動表單資料: " + req.toString());

        if (req != null) {
            Users creator = userRepository.findByAccount(req.getEventAddData().getAccount());
            //我是Date格式
            Date shelfTime = stringToDate(req.getEventAddData().getShelfTime());

            Date offSalefTime = stringToDate(req.getEventAddData().getOffSaleTime());

            try {
                Events events = new Events();
                events.setUserId(creator.getUserId());
                events.setEvnetsName(req.getEventAddData().getEventsName());
                events.setEventsDetails(req.getEventAddData().getEventsDetails());
                events.setEventsLocation(req.getEventAddData().getEventsLocation());
                events.setEventsOrganizer(req.getEventAddData().getEventsOrganizer());
                events.setEventDate(formatString(req.getEventAddData().getEventDate()));
                events.setShelfTime(shelfTime);
                events.setOffSaleTime(offSalefTime);
                events.setImage1(req.getEventAddData().getImage1());

                List<Area> areas = new ArrayList<>();
                for (AreaAddRequest data : req.getAreaAddData()) {
                    Area area = new Area();
                    area.setAreaName(data.getAreaName());
                    area.setAreaPrice(data.getAreaPrice());
                    area.setQty(data.getQty());
                    area.setEventsId(events);
                    areas.add(area);
                }
                events.setArea(areas);

                eventsRepository.save(events);
                System.out.println("eventsRepository已執行save");
                RestfulResponse<String> reponse = new RestfulResponse<>
                        ("0000", "新增活動成功", "接收到前端傳來的資料，感謝飛天小女警的幫忙");
                return reponse;
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

    @Override
    public RestfulResponse<Iterable<Events>> getEventsByUserId(FindUserByAccountRequst req) {
        Users users = userRepository.findByAccount(req.getAccount());
        List<Events> events = eventsRepository.getByUserId(users.getUserId());
        if (!events.isEmpty()){
            RestfulResponse<Iterable<Events>> response = new RestfulResponse<>("0000","搜尋成功",events);
            return  response;

        }
        RestfulResponse<Iterable<Events>> responseFail = new RestfulResponse<>("-0001","搜尋失敗",null);

        return responseFail;
    }
}

