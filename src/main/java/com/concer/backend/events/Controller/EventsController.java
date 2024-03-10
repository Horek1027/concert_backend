package com.concer.backend.events.Controller;

import com.concer.backend.Request.EventsRequest;
import com.concer.backend.Response.EventsResponse;
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
        RestfulResponse<Events> response =new RestfulResponse<Events>("0000","搜尋到全部資料",events.get());

        return response;
    }
    //使用關鍵字搜尋
    @GetMapping("/search/{input}")
        return  eventsService.wordSerchEvent(input);
    }

    //新增events包含area
    @PostMapping("/add")
    public RestfulResponse<String> insert(@RequestBody @Valid EventsRequest req){
        return eventsService.insert(req);
    }
}
