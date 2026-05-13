package com.concer.backend.Response;

import com.concer.backend.users.Entity.Users;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsersInfoResponse {

    private Users users;
    private String token;
}
