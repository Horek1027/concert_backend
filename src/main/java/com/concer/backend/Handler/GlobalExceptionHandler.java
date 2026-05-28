package com.concer.backend.Handler;

import com.concer.backend.Response.RestfulResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice //關鍵：告訴 Spring 這是全域的 Controller 攔截器
public class GlobalExceptionHandler {
    /**
     * @Valid 驗證失敗 (原本前端會收到 400 的情況)
     * 例如：account 是空值
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK) // 讓前端收到 HTTP 200，但從 code 自行判斷失敗
    public RestfulResponse<String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // 抓出第一個錯誤的欄位與你在 DTO 寫的 message
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String errorMessage = (fieldError != null) ? fieldError.getDefaultMessage() : "參數格式不正確";

        log.warn("【前端參數驗證失敗】欄位: {}, 原因: {}",
                (fieldError != null ? fieldError.getField() : "未知"), errorMessage);
        return new RestfulResponse<>("-0003", "請求參數驗證失敗", errorMessage);
    }

    /**
     * 網址輸入錯誤 / 找不到 API 路徑 (原本前端會收到 404 的情況)
     * 注意：需要在 application.properties 加上額外設定，此攔截才會生效（見下方說明）
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.OK)
    public RestfulResponse<String> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        log.warn("【找不到該 API 路徑】方法: {}, 網址: {}", ex.getHttpMethod(), ex.getRequestURL());

        return new RestfulResponse<>("-0004", "請求失敗", "您所請求的網址路徑不存在，請重新確認");
    }

    /**
     * 攔截系統所有未知的運行時異常 (原本前端會收到 500 的情況)
     * 例如：資料庫斷線、程式碼沒寫好突然踩到未知 NullPointerException
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public RestfulResponse<String> handleAllExceptions(Exception ex) {
        // 用 log.error 完整記錄黑盒子軌跡，方便後端排錯
        log.error("【系統發生未預期嚴重錯誤】: ", ex);

        // 絕對不要把 ex.getMessage() 餵給前端，保護資安
        return new RestfulResponse<>("-0099", "系統錯誤", "伺服器忙碌中，請稍後再試或聯絡管理員");
    }
}