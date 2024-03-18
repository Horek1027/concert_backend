package com.concer.backend.events.Service;

import com.concer.backend.Request.*;
import com.concer.backend.Response.EventsResponse;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.events.Entity.Events;

import java.util.List;
import java.util.Optional;

public interface EventsService {
    RestfulResponse<String> insert(EventsAndAreaRequest req1);

    RestfulResponse<Iterable<Events>> getAllEvents ();

    //單一搜尋
    Optional<Events> getEventsInfo (Integer eventsId);

    //關鍵字搜尋
    RestfulResponse<List<Events>> wordSerchEvent(String input);

    RestfulResponse<RestfulResponse> getEventsByUserId(FindUserByAccountRequst req);



}
