package com.concer.backend.area.Service;

import com.concer.backend.area.DAO.AreaRepository;
import com.concer.backend.area.Entity.Area;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AreaServiceImpl  implements AreaService {
    private AreaRepository areaRepository;

    @Override
    public void insert(List<Area> areas) {
        for(Area a :areas){
            areaRepository.save(a);
        }

    }
}
