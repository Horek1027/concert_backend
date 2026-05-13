package com.concer.backend.events.Controller;

import com.concer.backend.Request.EventsAndAreaRequest;
import com.concer.backend.Request.FindUserByAccountRequst;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.events.Entity.Events;
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
    public RestfulResponse<Iterable<Events>> getAllEvents(){

        return  eventsService.getAllEvents();
    }
    //使用id 單一搜尋
    @GetMapping("/{eventsId}")
    public RestfulResponse<Events> getEvnentInfo(@PathVariable Integer eventsId) {
        Optional<Events> events = eventsService.getEventsInfo(eventsId);
        if(events.isPresent()){
            RestfulResponse<Events> response =new RestfulResponse<Events>("0000","搜尋到單筆資料",events.get());
            return response;
        }
        RestfulResponse<Events> responsefail =new RestfulResponse<Events>("-0001","查無單筆資料",null);
        return responsefail;
    }
    //使用關鍵字搜尋
    @GetMapping("/search/{input}")
    public RestfulResponse<List<Events>> wordSerchEvent(@PathVariable String input){
        return  eventsService.wordSerchEvent(input);
    }

    //新增events包含area
    @PostMapping("/add")
    public RestfulResponse<String> insert(@RequestBody @Valid EventsAndAreaRequest req){
        return eventsService.insert(req);

    }
    @PostMapping("/userId")
    public RestfulResponse<Iterable<Events>>getEventsByUserId(@RequestBody FindUserByAccountRequst req){
        return eventsService.getEventsByUserId(req);
    }
}
