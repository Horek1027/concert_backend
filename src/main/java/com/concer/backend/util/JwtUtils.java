package com.concer.backend.util;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
public class JwtUtils {

    // 密鑰必須至少有 256 位元（32字元），建議從外部配置文件讀取
    private static final String SECRET_STRING = "your-very-safe-and-secret-key-at-least-32-chars";
    private static final Key SIGNING_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes());

    // 過期時間設定：10 分鐘 (10 * 60 * 1000 毫秒)
    private static final long EXPIRE_TIME = 10 * 60 * 1000;

    /**
     * 產生 Token
     */
    public static String createJwtToken(String account, Long userId) {
        long now = System.currentTimeMillis();
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);

        return Jwts.builder()
                .setClaims(claims)              // 放入自定義資訊
                .setSubject(account)            // 放入帳號作為主體
                .setIssuedAt(new Date(now))     // 簽發時間
                .setExpiration(new Date(now + EXPIRE_TIME)) // 設定 10 分鐘後過期
                .signWith(SIGNING_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析並驗證 Token
     * 如果過期或被竄改，此方法會直接拋出 Exception
     */
    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SIGNING_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
