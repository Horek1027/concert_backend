package com.concer.backend.events.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.concer.backend.Request.AreaAddRequest;
import com.concer.backend.Request.EventsAndAreaRequest;
import com.concer.backend.Request.FindUserByAccountRequst;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.area.Entity.Area;
import com.concer.backend.area.MyBatisPlus.MyBatisPlusAreaEntity;
import com.concer.backend.area.MyBatisPlus.MyBatisPlusAreaMapper;
import com.concer.backend.area.Service.AreaService;
import com.concer.backend.events.DAO.EventsRepository;
import com.concer.backend.events.Entity.Events;
import com.concer.backend.events.MyBatisPlus.MyBatisPlusEventsEntity;
import com.concer.backend.events.MyBatisPlus.MyBatisPlusEventsMapper;
import com.concer.backend.users.DAO.UserRepository;
import com.concer.backend.users.Entity.Users;
import com.concer.backend.users.MyBatisPlus.MyBatisPlusUsersEntity;
import com.concer.backend.users.MyBatisPlus.MyBatisPlusUsersMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.NullValueInNestedPathException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor // 自動為所有標記為 final 的欄位生成建構子

public class EventsServiceImpl implements EventsService {
//    private final EventsRepository eventsRepository;
//    private final UserRepository userRepository;
//    private final AreaRepository areaRepository;
//    private final EventsMapper eventsMapper;

    private final AreaService areaService;

    private final MyBatisPlusUsersMapper myBatisPlusUsersMapper;
    private final MyBatisPlusAreaMapper myBatisPlusAreaMapper;
    private final MyBatisPlusEventsMapper myBatisPlusEventsMapper;

    @Override
    public RestfulResponse<Iterable<MyBatisPlusEventsEntity>> getAllEvents() {
//        List<Events> list = eventsRepository.findAllWithArea();
        List<MyBatisPlusEventsEntity> list = myBatisPlusEventsMapper.findAllWithArea();
        return new RestfulResponse<>("0000", "搜尋到全部資料", list);
    }

    // 單一搜尋
    @Override
    public Optional<MyBatisPlusEventsEntity> getEventsInfo(Integer eventsId) {

        return myBatisPlusEventsMapper.findById(eventsId);
    }

    // 關鍵字搜尋
    @Override
    public RestfulResponse<List<MyBatisPlusEventsEntity>> wordSerchEvent(String input) {
        System.out.println("前端送來的搜尋字串:" + input);

        List<MyBatisPlusEventsEntity> eventsList = myBatisPlusEventsMapper.searchProgramInfoByName(input);
        if (eventsList.isEmpty()) {
            return new RestfulResponse<>("-0001", "關鍵字查無資料", eventsList);
        }
        return new RestfulResponse<>("0000", "關鍵字搜尋成功", eventsList);
    }

    // 下方Date 日期轉String 日期
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

    // 下方轉換字串日期格式
    public static String formatString(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date date = sdf.parse(dateString);

            // SimpleDateFormat changeFormat = new SimpleDateFormat("yyyy/M/d（E）HH:mm",
            // Locale.CHINESE);
            SimpleDateFormat changeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            changeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Taipei"));

            return changeFormat.format(date);
        } catch (ParseException e) {
            log.error("發生錯誤", e);
            return null;
        }
    }

    private MyBatisPlusUsersEntity findByAccount (String account){
        return  myBatisPlusUsersMapper.selectOne(
                new LambdaQueryWrapper<MyBatisPlusUsersEntity>()
                        .eq(MyBatisPlusUsersEntity::getAccount,account)
        );
    }

    @Override
    @Transactional //新增涉及多張 Table一定要加
    public RestfulResponse<String> insert(EventsAndAreaRequest req) {
        System.out.println("接收到的活動表單資料: " + req.toString());
//        Users creator = userRepository.findByAccount(req.getEventAddData().getAccount());
        MyBatisPlusUsersEntity creator = this.findByAccount(req.getEventAddData().getAccount());
        if (creator == null) {
            return new RestfulResponse<>("-0001", "活動新增失敗", "該會員帳號不存在");
        }

        try {
            // 我是Date格式轉換
            Date shelfTime = stringToDate(req.getEventAddData().getShelfTime());
            Date offSalefTime = stringToDate(req.getEventAddData().getOffSaleTime());

            MyBatisPlusEventsEntity events = new MyBatisPlusEventsEntity();
            events.setUserId(creator.getUserId());
            events.setEventsName(req.getEventAddData().getEventsName());
            events.setEventsDetails(req.getEventAddData().getEventsDetails());
            events.setEventsLocation(req.getEventAddData().getEventsLocation());
            events.setEventsOrganizer(req.getEventAddData().getEventsOrganizer());
            events.setEventDate(formatString(req.getEventAddData().getEventDate()));
            events.setShelfTime(shelfTime);
            events.setOffSaleTime(offSalefTime);
            events.setImage1(req.getEventAddData().getImage1());

            myBatisPlusEventsMapper.insert(events);

            List<MyBatisPlusAreaEntity> areas = new ArrayList<>();
            for (AreaAddRequest data : req.getAreaAddData()) {
                MyBatisPlusAreaEntity area = new MyBatisPlusAreaEntity();
                area.setAreaName(data.getAreaName());
                area.setAreaPrice(data.getAreaPrice());
                area.setQty(data.getQty());
                area.setEventsId(events.getEventsId());
                areas.add(area);
            }
            areaService.saveBatch(areas);
            //MyBatis 不能設定oneTwoMany 以及ManyToOne 因此要分兩次insert
//            eventsRepository.save(events);
//            System.out.println("eventsRepository已執行save");
            return new RestfulResponse<>("0000", "新增活動成功", null);
        } catch (NullValueInNestedPathException nullMessage) {
//            BeanUtils.getProperty(req, "eventAddData.eventsName")。如果中間的物件存在，
//            但目標字串反射失敗，或者中間某個自訂物件在轉換過程中斷掉了，就會噴出這個異常。
            log.warn("請求資料中包含未預期的空路徑: ", nullMessage);
            return new RestfulResponse<>("-0001", "req 資料有空值", null);
        } catch (Exception e) {
            log.error("新增活動時發生系統未預期錯誤: ", e);
            return new RestfulResponse<>("-0001", "活動新增失敗", "活動新增失敗");
        }
    }

    @Override
    public RestfulResponse<Iterable<MyBatisPlusEventsEntity>> getEventsByUserId(FindUserByAccountRequst req) {
        MyBatisPlusUsersEntity users = this.findByAccount(req.getAccount());
//        List<Events> events = eventsRepository.getByUserId(users.getUserId());
//        List<MyBatisPlusEventsEntity> events = myBatisPlusEventsMapper.selectList(
//                new LambdaQueryWrapper<MyBatisPlusEventsEntity>()
//                        .in(MyBatisPlusEventsEntity::getUserId,users.getUserId())
//        );

        List<MyBatisPlusEventsEntity> events = myBatisPlusEventsMapper.findUserCreatedEvent(users.getUserId());

//        log.info("查詢到的資料:" + events);


        if (!events.isEmpty()) {
            return new RestfulResponse<>("0000", "搜尋成功", events);
        }
        return  new RestfulResponse<>("-0001", "搜尋失敗", null);
    }
}
