package com.concer.backend.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsersLoginRequest{
    @NotBlank(message = "account不能是空值")
    private String account;
    @NotBlank(message = "password不能是空值")
    private String password;
}
