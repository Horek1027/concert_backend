package com.concer.backend.Selenium.Dto;

import lombok.Builder;
import lombok.Data;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

@Data
@Builder
public class RegisterPageDto {
    public String accountInput;
    public String passwordInput;
    public String rePasswordInput;
    public String nickNameInput;
    public String emailInput;
    public String phoneInput;
    public String status;


    // 在 DTO 內加上快速產出正確資料的方法
    public static RegisterPageDto createValidDto() {
        return RegisterPageDto.builder()
                .accountInput("validUser")
                .passwordInput("validUser")
                .rePasswordInput("validUser")
                .emailInput("test@gmail.com")
                .phoneInput("0988123456")
                .status("0")
                .build();
    }
}
