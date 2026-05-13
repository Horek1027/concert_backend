package com.concer.backend.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
 record 中，<T> 是一個泛型參數，它的作用是讓你在使用 RestfulResponse
 類別時可以指定一個具體的類型。
 這樣一來，RestfulResponse 就可以處理各種不同類型的數據，
 而不需要為每一種類型都定義一個不同的類別。
*/
//public record RestfulResponse<T> (
//        String returnCode,
//        String returnMsg,
//        T data
/* T data 代表泛型，可以靈活地處理不同類型的數據。*/
//){ }
/*Java 14 引入的 record 類型。record 是一種輕量級的數據類型，
它提供了一種簡潔的方式來聲明不可變的數據模型
RestfulResponse 是一個 record，它聲明了三個字段：
returnCode、returnMsg 和 data，這三個字段
分別用來表示 API 的返回代碼、返回訊息和返回的數據。
record 類型自動提供了簡單的建構函式、getter 和 toString 方法。
*/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestfulResponse<T> {
    String returnCode;
    String returnMsg;
    T data;
}