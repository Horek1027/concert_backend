package com.concer.backend.events.Service;

import java.util.List;
import java.util.Optional;

import com.concer.backend.Request.EventsAndAreaRequest;
import com.concer.backend.Request.FindUserByAccountRequst;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.events.Entity.Events;
import com.concer.backend.events.MyBatisPlus.MyBatisPlusEventsEntity;

public interface EventsService {
    RestfulResponse<String> insert(EventsAndAreaRequest req1);

    RestfulResponse<Iterable<MyBatisPlusEventsEntity>> getAllEvents ();

    //單一搜尋
    Optional<MyBatisPlusEventsEntity> getEventsInfo (Integer eventsId);

    //關鍵字搜尋
    RestfulResponse<List<MyBatisPlusEventsEntity>> wordSerchEvent(String input);

    RestfulResponse<Iterable<MyBatisPlusEventsEntity>> getEventsByUserId(FindUserByAccountRequst req);



}
