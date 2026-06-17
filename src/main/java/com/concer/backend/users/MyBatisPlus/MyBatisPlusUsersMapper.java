package com.concer.backend.users.MyBatisPlus;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Mapper
public interface MyBatisPlusUsersMapper extends BaseMapper<MyBatisPlusUsersEntity> {

    void updateUsersDetail(
            @Param("account") String account,
            @Param("nickname") String nickname,
            @Param("email") String email,
            @Param("cellphone") String cellphone
    );

//    @Modifying
//    @Query("UPDATE Users u SET u.password = :password WHERE u.account = :account")
//    void updatePassword( @Param("account") String account,
//                         @Param("password") String password);

}
