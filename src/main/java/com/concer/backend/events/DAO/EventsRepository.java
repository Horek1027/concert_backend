package com.concer.backend.events.DAO;

import com.concer.backend.Response.EventsResponse;
import com.concer.backend.events.Entity.Events;
import jakarta.persistence.criteria.CriteriaBuilder;
import jdk.jfr.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventsRepository extends JpaRepository<Events, Integer> {

    List<Events> getByUserId(Integer userId);
    //透過 JOIN FETCH，把 Events 連同肚子裡的 Area 一次查出來，SQL 執行次數會從 N+1 降為永遠的 1 次
    @Query("SELECT DISTINCT e FROM Events e LEFT JOIN FETCH e.area")
    List<Events> findAllWithArea();
    @Query("SELECT p FROM Events p WHERE p.eventsName LIKE %:input%")
    List<Events> searchProgramInfoByName(@Param("input") String input);

    //只找出販售中的活動
    @Query("SELECT e FROM Events e JOIN FETCH e.area WHERE e.offSaleTime > CURRENT_TIMESTAMP ORDER BY e.shelfTime DESC")
    List<Events> findAvailable();
}
