package com.concer.backend.Request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRequest {
    private Integer userId;
    private String account;
    private String password;
    private String nickname;
    private String email;
    private String cellphone;
    private Integer status;
}
