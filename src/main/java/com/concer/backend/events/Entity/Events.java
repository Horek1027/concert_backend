package com.concer.backend.events.Entity;


import com.concer.backend.area.Entity.Area;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="events")
public class Events {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="events_id")
    private Integer eventsId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "events_name")
    private String eventsName;

    @Column(name = "events_details")
    private String eventsDetails;

    @Column(name = "events_location")
    private String eventsLocation;

    @Column(name = "events_organizer")
    private String eventsOrganizer;

    @Column(name = "event_date")
    private String eventDate;

    @Column(name = "shelf_time")
    private Date shelfTime;

    @Column(name = "offsale_time")
    private Date offSaleTime;

    @Column(name = "image1")
    private String image1;

    @OneToMany(mappedBy = "eventsId",//連結的欄位名稱
            fetch=FetchType.LAZY,
            //除了remove都要附加
            cascade = { CascadeType.MERGE,CascadeType.MERGE,
                    CascadeType.REFRESH ,CascadeType.PERSIST})
    @JsonManagedReference
    private List<Area> area;

    /*在OneToMany的Entiy新增，ManyToOne的add方法
    * 方便新增Events的時候一起新增area同時做連結*/
    public void add(Area tempArea){
        if(area == null){
            area = new ArrayList<>();
        }
        area.add(tempArea);
    }
}
