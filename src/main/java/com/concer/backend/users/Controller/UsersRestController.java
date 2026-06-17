package com.concer.backend.users.Controller;

import com.concer.backend.Request.*;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.Response.UserAccountResponse;
import com.concer.backend.Response.UsersInfoResponse;
import com.concer.backend.Response.UsersLoginResponse;
import com.concer.backend.users.Entity.Users;
import com.concer.backend.users.MyBatisPlus.MyBatisPlusUsersEntity;
import com.concer.backend.users.Service.UsersService;
import com.concer.backend.users.Service.UsersServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UsersRestController {
    @Autowired
    private UsersService usersService;

    @PostMapping("/testDeviceApi")
    public RestfulResponse<String>testDeviceApi(
            @RequestHeader(value = "Sec-CH-UA-Mobile", required = false) String isMobileHeader,
            @RequestHeader(value = "User-Agent", required = false) String userAgent){
        boolean isMobile = checkIsMobile(isMobileHeader, userAgent);
        return usersService.testAPI(isMobile);
    }

    /**
     * 輔助方法：判斷是否為手機
     */
    private boolean checkIsMobile(String isMobileHeader, String userAgent) {
        // 優先使用現代瀏覽器的 Client Hints
        if (isMobileHeader != null) {
            return "?1".equals(isMobileHeader);
        }
        // 備援方案：使用 User-Agent 關鍵字判斷
        if (userAgent != null) {
            String ua = userAgent.toLowerCase();
            return ua.contains("android") || ua.contains("iphone") || ua.contains("mobile");
        }
        return false; // 預設為電腦端
    }

    @GetMapping("/findAll")
    public List<MyBatisPlusUsersEntity> getAllUsers(){
        return usersService.getAllUsers();
    }
    @PostMapping("/register")
    public RestfulResponse<String> insert(@RequestBody @Valid AddUsersRequest req){
        return usersService.insert(req);
    }

    @PostMapping("/login")
    public RestfulResponse<UsersLoginResponse>login(@RequestBody UsersLoginRequest req){
        return usersService.loginForSmallToken(req);
    }

    @PostMapping("/logout")
    public RestfulResponse<Void>logout(){
        return usersService.logout();
    }

    @PostMapping("/validate")
    public RestfulResponse<UsersLoginResponse>validateToken(@RequestBody UsersTokenRequest req ){
        return usersService.validateToken(req);

    }

    @PostMapping("/forceLogout")
    public RestfulResponse<String> forceLogout(@RequestBody UsersInfoRequest req) {
        System.out.println("管理員執行強制登出，帳號: " + req.getAccount());
        return usersService.forceLogout(req.getAccount());
    }


    @PostMapping("/search")
    public RestfulResponse<UserAccountResponse> getUserByAccount(@RequestBody UsersInfoRequest req){
        System.out.println("getUserByAccount，查詢帳號:" +req.getAccount());
        RestfulResponse<UserAccountResponse> response = usersService.getUsersByAccount(req);
        System.out.println("getUserByAccount，結果"+response);
        return response;
    }
    @PostMapping("/update")
    public  RestfulResponse<String> updateUsersDetail (@RequestBody UpdateUserRequest req){
        System.out.println("updateUsersDetail，更新帳號資料:" +req.getAccount());
        return usersService.updateUserDetail(req);
    }

    @PostMapping("/updatePassword")
    public RestfulResponse<String> updatePassowrd(@RequestBody UpdatePasswordRequest req){
        System.out.println("updatePassowrd，更新密碼資料:" +req.getAccount());
        return usersService.updateUserPassword(req);
    }
}
