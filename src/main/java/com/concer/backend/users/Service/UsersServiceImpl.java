package com.concer.backend.users.Service;

import com.concer.backend.Request.*;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.Response.UserAccountResponse;
import com.concer.backend.Response.UsersInfoResponse;
import com.concer.backend.Response.UsersLoginResponse;

import com.concer.backend.users.DAO.UserRepository;
import com.concer.backend.users.Entity.Users;
import com.concer.backend.util.Lock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Service
@Transactional
public class UsersServiceImpl implements UsersService {

    @Autowired
    private UserRepository userRepository;

    private BCryptPasswordEncoder passwordEncoder;

    @Autowired //
    public UsersServiceImpl(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public RestfulResponse<String> insert(AddUsersRequest req) {
        if (req != null && req.getAccount() != null) {
            Users checkAccount = userRepository.findByAccount(req.getAccount());
            if (checkAccount != null) {
                RestfulResponse<String> response = new RestfulResponse<String>
                        ("0003", "帳號已存在", null);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response).getBody();
            }
        }
        try {
            Users users = new Users();
            users.setAccount(req.getAccount());
            users.setPassword(passwordEncoder.encode(req.getPassword())); //把密碼設定為Bcry加密
            users.setEmail(req.getEmail());
            users.setNickname(req.getNickname());
            users.setCellphone(req.getCellphone());
            users.setStatus(req.getStatus());
            userRepository.save(users);
            System.out.println("userRepository執行save");

            RestfulResponse<String> Response =
                    new RestfulResponse<>("0000", "新增成功", "新增成功");
            return Response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        RestfulResponse<String> Responsefail =
                new RestfulResponse<>("-0001", "新增失敗", "新增失敗");
        return Responsefail;
    }

    @Override
    public Optional<Users> getUsersById(Integer usersId) {
        return userRepository.findById(usersId);
    }

    @Override
    public RestfulResponse<UserAccountResponse> getUsersByAccount(UsersInfoRequest req) {
        if (req.getAccount() != null) {
            Users users = userRepository.findByAccount(req.getAccount());
            if (users == null) {
                RestfulResponse<UserAccountResponse> responsefail =
                        new RestfulResponse<>("-0002", "查無此使用者", null);
                return responsefail;
            }
            UserAccountResponse result = new UserAccountResponse();
            result.setUsers(users);

            RestfulResponse<UserAccountResponse> response =
                    new RestfulResponse<>("0000", "搜尋成功", result);
            return response;
        }
        RestfulResponse<UserAccountResponse> responsefail =
                new RestfulResponse<>("-0001", "搜尋失敗", null);
        return responsefail;
    }

    @Override
    public RestfulResponse<UsersLoginResponse> login(UsersLoginRequest req) {
        if (req.getAccount() != null && req.getPassword() != null) {
            System.out.println("收到要求資料:" + req);
            String inputAccount = req.getAccount();

            //確認是否有此筆資料
            Users users = userRepository.findByAccount(inputAccount);
            System.out.println("找到的帳號:" + users);

            //下方比對密碼
            if (users != null && Lock.checkPasssword(req.getPassword(), users.getPassword())) {
                String token = Lock.encryptUsers(users);

                UsersLoginResponse usersLoginResponse = new UsersLoginResponse();
                usersLoginResponse.setToken(token);
                usersLoginResponse.setAccount(inputAccount);

                RestfulResponse<UsersLoginResponse> response =
                        new RestfulResponse<>("0000", "登入成功", usersLoginResponse);
                return response;
            }

            System.out.println("密碼比對失敗");
            RestfulResponse<UsersLoginResponse> responseFail =
                    new RestfulResponse<>("-0001", "登入失敗", null);
            return responseFail;
        }
        RestfulResponse<UsersLoginResponse> responseFail =
                new RestfulResponse<>("-0001", "登入失敗", null);
        return responseFail;
    }

    @Override
    public RestfulResponse<UsersLoginResponse> validateToken(UsersTokenRequest req) {
        //1.確認是否為會員
        Users orginalUser = userRepository.findByAccount(req.getAccount());
        System.out.println(orginalUser);

        //2.把從req.getAccount得到的會員資料轉token(加密為token)，跟傳入的token做比對
        //&&兩邊都要true 才會驗證成功，orginlUser =DB拉出來的資料
        //token?
        if (orginalUser != null && Lock.checkUsers(orginalUser, req.getToken())) {

            UsersLoginResponse usersLoginResponse = new UsersLoginResponse(req.getToken(), req.getAccount());
            RestfulResponse<UsersLoginResponse> response =
                    new RestfulResponse<>("0000", "驗證成功", usersLoginResponse);
            System.out.println("我有到validateToken內");
            return response;
        }
        UsersLoginResponse usersLoginResponse = new UsersLoginResponse(req.getToken(), req.getAccount());
        RestfulResponse<UsersLoginResponse> responsefail =
                new RestfulResponse<>("-0001", "驗證失敗", usersLoginResponse);
        return responsefail;

    }

    @Override
    public RestfulResponse<String> updateUserDetail(UpdateUserRequest req) {
        try {
            userRepository.updateUsersDetail(req.getAccount(), req.getNickname(),
                    req.getEmail(), req.getCellphone());
            System.out.println("userRepository執行DetailUpdate");

            RestfulResponse<String> Response =
                    new RestfulResponse<>("0000", "基本資料修改成功", "基本資料修改成功");
            return Response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        RestfulResponse<String> Responsefail =
                new RestfulResponse<>("-0001", "修改失敗", "修改失敗");
        return Responsefail;
    }

    @Override
    public RestfulResponse<String> updateUserPassword(UpdatePasswordRequest req) {

        System.out.println("收到要求資料:" + req);
        String inputAccount = req.getAccount();
        Users users = userRepository.findByAccount(inputAccount);

        //下方比對密碼是否相同，相同則跳出錯誤
        if (users != null && !Lock.checkPasssword(req.getPassword(), users.getPassword())) {
            System.out.println("舊密碼輸入錯誤");
            RestfulResponse<String> Responsefail =
                    new RestfulResponse<>("-0003", "舊密碼輸入錯誤",
                            "，舊密碼不是:" + req.getPassword());
            return Responsefail;
        } else {
            try {
                System.out.println("執行修改密碼區塊");
                userRepository.updatePassword(req.getAccount(), passwordEncoder.encode(req.getNewPassword()));

                RestfulResponse<String> Response =
                        new RestfulResponse<>("0000", "密碼修改成功",
                                "密碼修改成功"+req.getNewPassword());
                return Response;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        RestfulResponse<String> Responsefail =
                new RestfulResponse<>("-0001", "密碼修改失敗",
                        "密碼輸入錯誤");
        return Responsefail;
    }

    @Override
    public List<Users> getAllUsers() {

        return userRepository.findAll();
    }
}
