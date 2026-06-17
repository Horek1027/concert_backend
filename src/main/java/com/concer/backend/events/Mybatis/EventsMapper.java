package com.concer.backend.events.Mybatis;

import com.concer.backend.events.Entity.Events;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Optional;

@Mapper
public interface EventsMapper {
    // 只需定義方法，不用寫 SQL
    List<Events> findAllWithArea();
    List<Events> searchProgramInfoByName(@Param("input") String input);
    Optional<Events> findById(@Param("eventsId") Integer eventsId);
}
