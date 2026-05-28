package com.concer.backend.users.Service;

import com.concer.backend.Request.*;
import com.concer.backend.Response.RestfulResponse;
import com.concer.backend.Response.UserAccountResponse;
import com.concer.backend.Response.UsersLoginResponse;
import com.concer.backend.users.DAO.UserRepository;
import com.concer.backend.users.Entity.Users;
import com.concer.backend.util.JwtUtils;
import com.concer.backend.util.Lock;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor // 自動為所有標記為 final 的欄位生成建構子
@Transactional
@Slf4j
public class UsersServiceImpl implements UsersService {
    private final UserRepository userRepository;
    // 官方的SpringRedis 套件 用於紀錄Session
    private final StringRedisTemplate stringRedisTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

//    public UsersServiceImpl(BCryptPasswordEncoder passwordEncoder ,UserRepository userRepository) {
//        this.userRepository = userRepository;
//        this.passwordEncoder = passwordEncoder;
//    }


    @Override
    public RestfulResponse<String> insert(AddUsersRequest req) {
        if (req == null || req.getAccount() == null) {
            return new RestfulResponse<>("-0001", "請求資料不完整", null);
        }

        Users checkAccount = userRepository.findByAccount(req.getAccount());
        if (checkAccount != null) {
            log.info("重複的帳號: " + checkAccount.getAccount());
            RestfulResponse<String> response = new RestfulResponse<>
                    ("0003", "帳號已存在",null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response).getBody();
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

            return new RestfulResponse<>("0000", "新增成功", "新增成功");
        } catch (Exception e) {
            log.error("新增會員發生錯誤:", e);
            return new RestfulResponse<>("-0001", "新增失敗", "新增失敗");
        }
    }

    @Override
    public Optional<Users> getUsersById(Integer usersId) {
        return userRepository.findById(usersId);
    }

    @Override
    public RestfulResponse<UserAccountResponse> getUsersByAccount(UsersInfoRequest req) {
        if (req.getAccount() == null) {
            return new RestfulResponse<>("-0001", "Account是空值", null);
        }
        try {
            Users users = userRepository.findByAccount(req.getAccount());
            if (users == null) {
                return new RestfulResponse<>("-0002", "查無此使用者", null);
            }
            return new RestfulResponse<>("0000", "搜尋成功", new UserAccountResponse(users));
        } catch (Exception e) {
            log.error("查詢出現異常", e);
            return new RestfulResponse<>("-0001", "搜尋失敗", null);
        }
    }

    private static final long ACCESS_TOKEN_EXPIRE_TIME = 10L * 60 * 1000;//設定10分鐘
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 7L * 24 * 60 * 60 * 1000;//例如 7 天，效期：7 * 24 * 60 * 60 秒 = 604800

    @Override
    public RestfulResponse<UsersLoginResponse> loginForSmallToken(UsersLoginRequest req) {
        //確認是否有此筆資料
        Users users = userRepository.findByAccount(req.getAccount());

        //下方比對密碼
        if (users != null && Lock.checkPasssword(req.getPassword(), users.getPassword())) {
            //產生給前端的10分鐘短效Token
            String token = JwtUtils.createJwtToken(users.getAccount(), Long.valueOf(users.getUserId()), ACCESS_TOKEN_EXPIRE_TIME);
            UsersLoginResponse usersLoginResponse = new UsersLoginResponse();
            usersLoginResponse.setToken(token);
            usersLoginResponse.setAccount(users.getAccount());
            return new RestfulResponse<>("0000", "登入成功", usersLoginResponse);
        }
        System.out.println("密碼比對失敗");
        return new RestfulResponse<>("-0001", "密碼比對失敗", null);
    }



    @Override
    public RestfulResponse<UsersLoginResponse> login(UsersLoginRequest req) {

        if (req.getAccount() != null && req.getPassword() != null) {
            String inputAccount = req.getAccount();

            //確認是否有此筆資料
            Users users = userRepository.findByAccount(inputAccount);

            //下方比對密碼
            if (users != null && Lock.checkPasssword(req.getPassword(), users.getPassword())) {
                //產生給前端的10分鐘短效Token
                String token = JwtUtils.createJwtToken(users.getAccount(), Long.valueOf(users.getUserId()), ACCESS_TOKEN_EXPIRE_TIME);
                UsersLoginResponse usersLoginResponse = new UsersLoginResponse();
                usersLoginResponse.setToken(token);
                usersLoginResponse.setAccount(inputAccount);

                //產生給後端的七天長效Token
                String jti = UUID.randomUUID().toString(); // 產生此 Token 的獨一無二 ID

                //1：記得把新加的 jti 參數傳進去，讓它呼叫到版本 B
                String refreshToken = JwtUtils.createJwtToken(users.getAccount(), Long.valueOf(users.getUserId()), jti, REFRESH_TOKEN_EXPIRE_TIME);

                // 2：使用 Duration.ofMillis 寫入 Redis，解決你的 set 噴錯問題
                stringRedisTemplate.opsForValue().set(
                        "user_session:" + users.getAccount(),
                        jti,
                        java.time.Duration.ofMillis(REFRESH_TOKEN_EXPIRE_TIME)
                );

                // 3. 將 RefreshToken 寫入 HttpOnly Cookie
                // 先將 attributes 取出並做 null 檢查，避免 getRequestAttributes() 為 null 時直接鏈式呼叫產生 NullPointerException
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                HttpServletResponse response = (attributes != null) ? attributes.getResponse() : null;
                if (response != null) {
                    ResponseCookie cookie = org.springframework.http.ResponseCookie.from("refreshToken", refreshToken)
                            .httpOnly(true)
                            .secure(true)
                            .path("/")
                            .maxAge(604800)
                            .sameSite("None")
                            .build();
                    response.setHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
                }
                return new RestfulResponse<>("0000", "登入成功", usersLoginResponse);
            }

            System.out.println("密碼比對失敗");
            return new RestfulResponse<>("-0001", "登入失敗", null);
        }

        return new RestfulResponse<>("-0001", "登入失敗", null);
    }





    @Override
    public RestfulResponse<UsersLoginResponse> validateToken(HttpServletRequest request) {
        // 1. 檢查 request 是否為空（防禦性程式設計）
        if (request == null) {
            return new RestfulResponse<>("-0001", "系統錯誤：無法取得請求上下文", null);
        }

        // 2. 從 Header 取得短效 Access Token
        String authHeader = request.getHeader("Authorization");
        String accessToken ;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7); // 除去 "Bearer " 取出純 Token
        } else {
            accessToken = authHeader; // 彈性處理：若前端只傳純字串
        }

        // 3. 從 Cookie 取得長效 Refresh Token
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        // 4. 基礎檢查：長效憑證是安全續期的底線，絕對不可缺少
        if (refreshToken == null || refreshToken.isEmpty()) {
            return new RestfulResponse<>("-0001", "請重新登入 (未偵測到長效憑證)", null);
        }

        try {
            String accessAccount = null;
            // 5. 彈性驗證短效 Token (Access Token)
            if (accessToken != null && !accessToken.isEmpty()) {
                try {
                    // 嘗試正常解析短效 Token
                    Claims accessClaims = JwtUtils.parseToken(accessToken);
                    accessAccount = accessClaims.getSubject();
                } catch (io.jsonwebtoken.ExpiredJwtException e) {
                    // 🌟 實務優化：如果短效 Token 已經過期，會拋出此異常。
                    // 我們依然可以透過 e.getClaims() 拿到過期 Token 內的資料來做交叉比對，這在續期機制中是合法的。
                    accessAccount = e.getClaims().getSubject();
                    log.info("接收到已過期的 Access Token，嘗試解出帳戶：{}", accessAccount);
                } catch (Exception e) {
                    // 如果是簽章錯誤、被篡改等異常，則直接視為無效 Token
                    log.warn("Access Token 解析發生非過期異常（可能遭竄改）", e);
                    return new RestfulResponse<>("-0001", "憑證異常，請重新登入", null);
                }
            }

            // 6. 驗證 Cookie 裡面的長效 Refresh Token（若過期或被改會直接拋異常進入最下方的 catch）
            Claims refreshClaims = JwtUtils.parseToken(refreshToken);
            String cookieAccount = refreshClaims.getSubject();
            String tokenJti = refreshClaims.getId(); // 取得這顆 Token 裡面的 JTI

            // 7. 安全交叉比對：防禦 CSRF
            // 如果 accessToken 有值（代表是前端定時背景刷新），就必須嚴格比對兩者帳號是否一致
            // 如果 accessToken 為 null（代表使用者按 F5 重新整理導致記憶體清空），則跳過此比對，直接走後續的 Cookie + Redis 驗證
            if (accessAccount != null && !accessAccount.equals(cookieAccount)) {
                log.warn("安全性警告：短效 Token 帳號 ({}) 與長效 Token 帳號 ({}) 不匹配！", accessAccount, cookieAccount);
                return new RestfulResponse<>("-0001", "憑證異常，請重新登入", null);
            }

            // 8. 核心狀態防禦：去 Redis 檢查該使用者的特定 Session 是否還活著（防禦登出、撤銷情境）
            String validJti = stringRedisTemplate.opsForValue().get("user_session:" + cookieAccount);

            // 如果 Redis 查不到，或是裡面的 JTI 跟現在傳過來的對不上，代表此憑證已被後端註銷
            if (validJti == null || !validJti.equals(tokenJti)) {
                log.warn("帳戶 {} 的長效憑證已被後端註銷或不匹配，拒絕刷新", cookieAccount);
                return new RestfulResponse<>("-0001", "憑證已失效，請重新登入", null);
            }

            // 9. 確認資料庫中是否真有此會員（身分一切以 Cookie/Token 安全解碼內容為核心）
            Users orginalUser = userRepository.findByAccount(cookieAccount);
            if (orginalUser == null) {
                return new RestfulResponse<>("-0001", "會員不存在", null);
            }

            // 10. 驗證完全通過，產生一張全新短效的 Access Token
            String newAccessToken = JwtUtils.createJwtToken(
                    orginalUser.getAccount(),
                    Long.valueOf(orginalUser.getUserId()),
                    ACCESS_TOKEN_EXPIRE_TIME
            );

            // 回傳給前端，account 也要用資料庫查出來的最新資料
            UsersLoginResponse usersLoginResponse = new UsersLoginResponse(newAccessToken, orginalUser.getAccount());

            if (accessToken == null) {
                log.info("帳戶 {} 因網頁重載(F5)觸發刷新，通過 Cookie + Redis 驗證成功", cookieAccount);
            } else {
                log.info("帳戶 {} 觸發定時無感刷新，通過長短 Token 交叉驗證與 Redis 驗證成功", cookieAccount);
            }

            return new RestfulResponse<>("0000", "驗證成功", usersLoginResponse);

        } catch (Exception e) {
            log.error("Token 同時驗證失敗、簽章錯誤或長效憑證已過期", e);
            return new RestfulResponse<>("-0001", "憑證已過期或無效，請重新登入", null);
        }
    }


    @Override
    public  RestfulResponse<UsersLoginResponse> validateToken(UsersTokenRequest req){
        try {
            Claims accessClaims = JwtUtils.parseToken(req.getToken());
            String accessAccount = accessClaims.getSubject();

            if (!req.getAccount().equals(accessAccount)) {
                return new RestfulResponse<>("-0001", "Token 帳號不一致", null);
            }

            Users originalUser = userRepository.findByAccount(accessAccount);
            if (originalUser == null) {
                return new RestfulResponse<>("-0001", "使用者不存在", null);
            }

            String newAccessToken = JwtUtils.createJwtToken(
                    originalUser.getAccount(),
                    Long.valueOf(originalUser.getUserId()),
                    ACCESS_TOKEN_EXPIRE_TIME
            );

            return new RestfulResponse<>(
                    "0000",
                    "刷新成功",
                    new UsersLoginResponse(newAccessToken, originalUser.getAccount())
            );

        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return new RestfulResponse<>("-0001", "Token 已過期，請重新登入", null);
        } catch (Exception e) {
            return new RestfulResponse<>("-0001", "Token 無效，請重新登入", null);
        }
    }

    @Override
    public RestfulResponse<Void> logout() { // 使用到長效toekn(存在Redis) 的才需要使用
        // 1. 取得 Request 與 Response 物件
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            HttpServletResponse response = attributes.getResponse();

            // 2. 核心安全操作：從 Request 中找出 RefreshToken，並註銷 Redis Session
            if (request.getCookies() != null) {
                String refreshToken = null;
                for (Cookie cookie : request.getCookies()) {
                    if ("refreshToken".equals(cookie.getName())) {
                        refreshToken = cookie.getValue();
                        break;
                    }
                }

                // 如果有找到 token，解析帳號並刪除 Redis key
                if (refreshToken != null && !refreshToken.isEmpty()) {
                    try {
                        Claims claims = JwtUtils.parseToken(refreshToken);
                        String account = claims.getSubject();

                        // 🌟 刪除 Redis 中的會話狀態，防止該 token 被二次使用
                        stringRedisTemplate.delete("user_session:" + account);
                        log.info("帳戶 {} 登出成功，已從 Redis 註銷會話", account);
                    } catch (Exception e) {
                        // 即使 Token 解析失敗（例如可能已經過期），也記錄警告，並繼續執行清除 Cookie 的動作
                        log.warn("登出時解析 Token 失敗，可能已過期：{}", e.getMessage());
                    }
                }
            }

            // 3. 建立一個同名、但 MaxAge = 0 的 ResponseCookie (使用 ResponseCookie 確保 SameSite 與 login 一致)
            if (response != null) {
                ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                        .httpOnly(true)
                        .secure(true)
                        .path("/")
                        .maxAge(0) // 🌟 設為 0 叫瀏覽器立刻刪除
                        .sameSite("None") // 🌟 關鍵：必須與 login 一致
                        .build();
                response.setHeader(org.springframework.http.HttpHeaders.SET_COOKIE, cookie.toString());
            }
        }
        return new RestfulResponse<>("0000", "登出成功", null);
    }


    @Override
    public RestfulResponse<String> forceLogout(String account) {
        if (account == null || account.isEmpty()) {
            return new RestfulResponse<>("-0001", "帳號不可為空", null);
        }
        String redisKey = "user_session:" + account;

        // 直接從 Redis 中刪除該使用者的 Session 紀錄
        Boolean deleted = stringRedisTemplate.delete(redisKey);

        if (Boolean.TRUE.equals(deleted)) {
            log.info("管理員已強制註銷帳戶 {} 的 Redis Session", account);
            return new RestfulResponse<>("0000", "已成功強制註銷該使用者的登入狀態", "註銷成功");
        } else {
            log.info("嘗試強制註銷帳戶 {}，但 Redis 中無此 Session (使用者可能未登入)", account);
            return new RestfulResponse<>("-0002", "該使用者目前未登入或無使用的session", "無使用的session");
        }
    }

    @Override
    public RestfulResponse<String> updateUserDetail(UpdateUserRequest req) {
        try {
            userRepository.updateUsersDetail(req.getAccount(), req.getNickname(),
                    req.getEmail(), req.getCellphone());
            System.out.println("userRepository執行DetailUpdate");
            return new RestfulResponse<>("0000", "基本資料修改成功", "基本資料修改成功");
        } catch (Exception e) {
            log.error("發生未能預期的錯誤",e);
            return  new RestfulResponse<>("-0001", "修改失敗", "修改失敗");
        }
    }

    @Override
    public RestfulResponse<String> updateUserPassword(UpdatePasswordRequest req) {

        System.out.println("收到要求資料:" + req);
        String inputAccount = req.getAccount();
        Users users = userRepository.findByAccount(inputAccount);

        //下方比對密碼是否相同，相同則跳出錯誤
        if (users != null && !Lock.checkPasssword(req.getPassword(), users.getPassword())) {
            return new RestfulResponse<>("-0003", "舊密碼輸入錯誤",
                    "，舊密碼不是:" + req.getPassword());
        } else {
            try {
                System.out.println("執行修改密碼區塊");
                userRepository.updatePassword(req.getAccount(), passwordEncoder.encode(req.getNewPassword()));
                return new RestfulResponse<>("0000", "密碼修改成功",
                        "密碼修改成功");
            } catch (Exception e) {
                log.error("修改密碼發生不確定錯誤:", e);
                return new RestfulResponse<>("-0001", "密碼修改失敗",
                        "發生不確定錯誤");
            }
        }
    }

    @Override
    public List<Users> getAllUsers() {

        return userRepository.findAll();
    }
}
