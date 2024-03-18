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

    @Query("SELECT p FROM Events p WHERE p.evnetsName LIKE %:input%")
    List<Events> searchProgramInfoByName(@Param("input") String input);

//    @Query("SELECT e FROM Events e WHERE e.offSaleTime > CURRENT_TIMESTAMP ORDER BY e.shelfTime DESC")
    @Query("SELECT e FROM Events e JOIN FETCH e.area WHERE e.offSaleTime > CURRENT_TIMESTAMP ORDER BY e.shelfTime DESC")
    List<Events> findAvailable();
}
