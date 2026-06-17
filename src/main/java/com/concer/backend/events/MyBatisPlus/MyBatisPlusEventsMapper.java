package com.concer.backend.events.MyBatisPlus;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.concer.backend.events.Entity.Events;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface MyBatisPlusEventsMapper extends BaseMapper<MyBatisPlusEventsEntity> {
    // 繼承了 BaseMapper 後，內建的 insert, delete, update, selectById 等方法都自動具備了！

    List<MyBatisPlusEventsEntity> findAllWithArea();
    List<MyBatisPlusEventsEntity> searchProgramInfoByName(@Param("input") String input);
    Optional<MyBatisPlusEventsEntity> findById(@Param("eventsId") Integer eventsId);

    List<MyBatisPlusEventsEntity> findUserCreatedEvent(@Param("userId") Integer userId);

}
