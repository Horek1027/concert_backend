package com.concer.backend.users.Service;

import com.concer.backend.Request.*;

import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.Response.UserAccountResponse;
import com.concer.backend.Response.UsersLoginResponse;


import com.concer.backend.users.Entity.Users;

import java.util.List;
import java.util.Optional;

public interface UsersService {

    RestfulResponse<String> insert(AddUsersRequest req);

    Optional<Users> getUsersById(Integer usersId);

    RestfulResponse<UserAccountResponse> getUsersByAccount (UsersInfoRequest req);

    List<Users> getAllUsers();

    RestfulResponse<UsersLoginResponse> login(UsersLoginRequest req);

    RestfulResponse<UsersLoginResponse> validateToken(UsersTokenRequest req);

    RestfulResponse<String> updateUserDetail (UpdateUserRequest req);

    RestfulResponse<String> updateUserPassword (UpdatePasswordRequest req);



}
