package com.concer.backend.events.Controller;

import com.concer.backend.Request.EventsAndAreaRequest;
import com.concer.backend.Request.FindUserByAccountRequst;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.events.Entity.Events;
import com.concer.backend.events.MyBatisPlus.MyBatisPlusEventsEntity;
import com.concer.backend.events.Service.EventsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/events")
public class EventsController {
    @Autowired
    private EventsService eventsService;
    //使用全部搜尋
    @GetMapping()
    public RestfulResponse<Iterable<MyBatisPlusEventsEntity>> getAllEvents(){
        return  eventsService.getAllEvents();
    }
    //使用id 單一搜尋
    @GetMapping("/{eventsId}")
    public RestfulResponse<MyBatisPlusEventsEntity> getEvnentInfo(@PathVariable Integer eventsId) {
        Optional<MyBatisPlusEventsEntity> events = eventsService.getEventsInfo(eventsId);
        return events.map(myBatisPlusEventsEntity -> new RestfulResponse<>("0000", "搜尋到單筆資料", myBatisPlusEventsEntity)).orElseGet(() -> new RestfulResponse<>("-0001", "查無單筆資料", null));
    }
    //使用關鍵字搜尋
    @GetMapping("/search/{input}")
    public RestfulResponse<List<MyBatisPlusEventsEntity>> wordSerchEvent(@PathVariable String input){
        return  eventsService.wordSerchEvent(input);
    }
    //新增events包含area
    @PostMapping("/add")
    public RestfulResponse<String> insert(@RequestBody @Valid EventsAndAreaRequest req){
        return eventsService.insert(req);
    }
    @PostMapping("/userId")
    public RestfulResponse<Iterable<MyBatisPlusEventsEntity>>getEventsByUserId(@RequestBody FindUserByAccountRequst req){
        return eventsService.getEventsByUserId(req);
    }
}
