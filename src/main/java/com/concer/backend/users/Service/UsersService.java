package com.concer.backend.users.Service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.concer.backend.Request.*;

import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.Response.UserAccountResponse;
import com.concer.backend.Response.UsersLoginResponse;


import com.concer.backend.area.MyBatisPlus.MyBatisPlusAreaEntity;
import com.concer.backend.users.Entity.Users;
import com.concer.backend.users.MyBatisPlus.MyBatisPlusUsersEntity;
import com.concer.backend.users.MyBatisPlus.MyBatisPlusUsersMapper;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Optional;

public interface UsersService extends IService<MyBatisPlusUsersEntity> {

    RestfulResponse<String>testAPI(boolean isMobile) ;

    RestfulResponse<String> insert(AddUsersRequest req);

    Optional<MyBatisPlusUsersEntity>  getUsersById(Integer usersId);

    RestfulResponse<UserAccountResponse> getUsersByAccount (UsersInfoRequest req);

    List<MyBatisPlusUsersEntity> getAllUsers();

    RestfulResponse<UsersLoginResponse> login(UsersLoginRequest req);

     RestfulResponse<UsersLoginResponse> loginForSmallToken(UsersLoginRequest req) ;

    RestfulResponse<Void> logout();
    RestfulResponse<String> forceLogout(String account);


    RestfulResponse<UsersLoginResponse> validateToken(HttpServletRequest request);

    RestfulResponse<UsersLoginResponse> validateToken(UsersTokenRequest req);

    RestfulResponse<String> updateUserDetail (UpdateUserRequest req);

    RestfulResponse<String> updateUserPassword (UpdatePasswordRequest req);



}
