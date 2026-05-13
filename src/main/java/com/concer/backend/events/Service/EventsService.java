package com.concer.backend.events.Service;

import java.util.List;
import java.util.Optional;

import com.concer.backend.Request.EventsAndAreaRequest;
import com.concer.backend.Request.FindUserByAccountRequst;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.events.Entity.Events;

public interface EventsService {
    RestfulResponse<String> insert(EventsAndAreaRequest req1);

    RestfulResponse<Iterable<Events>> getAllEvents ();

    //單一搜尋
    Optional<Events> getEventsInfo (Integer eventsId);

    //關鍵字搜尋
    RestfulResponse<List<Events>> wordSerchEvent(String input);

    RestfulResponse<Iterable<Events>> getEventsByUserId(FindUserByAccountRequst req);



}
