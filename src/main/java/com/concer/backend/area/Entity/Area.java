package com.concer.backend.area.Entity;

import com.concer.backend.events.Entity.Events;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="area")
public class Area {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="area_id")
    private Integer areaId;

    @ManyToOne(cascade = { CascadeType.MERGE,CascadeType.MERGE,
            CascadeType.REFRESH ,CascadeType.PERSIST})
    @JoinColumn(name="events_id")
    @JsonBackReference
    private Events eventsId;

    @Column(name = "area_name")
    private String areaName;

    @Column(name = "area_price")
    private Integer areaPrice;

    @Column(name = "qty")
    private Integer qty;

}
