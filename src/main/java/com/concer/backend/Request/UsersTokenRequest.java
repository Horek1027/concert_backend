package com.concer.backend.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsersTokenRequest{
    @NotBlank(message = "account物件不可為空")
    private String account;
    @NotBlank(message = "token物件不可為空")
    private String token;
}
