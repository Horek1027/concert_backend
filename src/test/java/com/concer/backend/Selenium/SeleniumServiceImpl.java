package com.concer.backend.Selenium;

import com.concer.backend.Selenium.Dto.AddAreaDto;
import com.concer.backend.Selenium.Dto.AddEventPageDto;
import com.concer.backend.Selenium.Dto.RegisterPageDto;
import com.concer.backend.Selenium.Page.AddAreaPage;
import com.concer.backend.Selenium.Page.AddEventPage;
import com.concer.backend.Selenium.Page.LoginPage;
import com.concer.backend.Selenium.Page.RegisterPage;
import org.junit.jupiter.api.*;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)// 關鍵：讓整個類別共用同一個測試實例與狀態
public class SeleniumServiceImpl {

    //test 環境下只能使用Autowired注入 不能使用建構子
    @Autowired
    private WebDriver webDriver;

    @Test
    @Order(1)
    public void register() {
        webDriver.get("http://localhost:5173/register");
        RegisterPage registerPage = new RegisterPage(webDriver);

        //呼叫方法，輸入參數
//        registerPage.register("userABCD","userABCD","userABCD","userABCD_test",
//                "userABCD@gmail.com","0912345678","1");
        // ❌ 注意：方法結尾絕對「不要」呼叫 webDriver.quit();
        //.accountInput("user_" + System.currentTimeMillis()) //這樣每次跑測試都會是全新的帳號，不會因為資料重複而失敗！
        RegisterPageDto registerPageDto = RegisterPageDto.builder().accountInput("autoTestUser")
                .passwordInput("autoTestUser")
//                .rePasswordInput("autoTestUser")
                .rePasswordInput("ABCDEFGHK")
                .nickNameInput("autoTestUser_test")
                .emailInput("userABCD@gmail.com")
                .phoneInput("0912345678").status("1").build();
        registerPage.registerByDto(registerPageDto);

        // 斷言：檢查畫面是否確實出現預期的錯誤提示
        String expectedMsg = "兩次密碼輸入不一致"; // 根據你前端實際文字修改
        String actualMsg = registerPage.getErrorMessageText();

        try {
            // 進行斷言比對
            Assertions.assertEquals(expectedMsg, actualMsg,
                    "【測試失敗說明】: 預期應被前端攔截並顯示「" + expectedMsg + "」，但畫面未出現或文字不符！");
            // 1. 如果成功點過來，拍一張成功的照片
            captureScreen("register_success");
        } catch (AssertionError e) {
            // 2. 如果斷言失敗（沒攔截到或字錯了），立刻拍下失敗現場！
            captureScreen("register_failed");
            // ❌ 記得把錯誤往外丟，不然 JUnit 會誤以為這條測試是成功的（綠燈）
            throw e;
        }
    }


    @Test
    @Order(2)
    public void loginTest() throws InterruptedException {
        // 1. 開啟網頁
//      webDriver.get("http://localhost:5173/login");
        webDriver.navigate().to("http://localhost:5173/login");
        // 2. 實例化 Page Object
        LoginPage loginPage = new LoginPage(webDriver);
        loginPage.login("autoTestUser", "autoTestUser");
//        Thread.sleep(10000);

    }

    @Test
    @Order(3)
    public void addEventTest() throws InterruptedException {
        webDriver.get("http://localhost:5173/admin/event-add");

        // 2. 實例化 Page Object
        AddEventPage addEventPage = new AddEventPage(webDriver);

        //上傳圖片
        // 準備你要上傳的圖片絕對路徑 (請根據你的電腦環境修改路徑)
        // Windows 範例: "C:\\tests\\resources\\cover.png"
        // Mac/Linux 範例: "/Users/yourname/tests/resources/cover.png"
        String imgFilePath = "C:\\Users\\2400148\\Pictures\\Screenshots\\test圖片.png";

        AddEventPageDto dto = AddEventPageDto.builder()
                .eventName("自動化測試")
                .fileUploadInput(imgFilePath)
                .eventDetail("做自動化測試ING")
                .host("自動化測試進行中")
                .eventDate("2026/06/01 19:00:00")
                .shelfTimeDate("2026/05/24 19:00:00")
                .offTimeDate("2026/06/01 00:00:00")
                .build();

        addEventPage.AddEventPagTest(dto);
    }

    @Test
    @Order(4)
    public void addAreaTest() throws InterruptedException {
//      webDriver.get("http://localhost:5173/admin/area-add"); //開啟新頁面
        AddAreaPage seatPage = new AddAreaPage(webDriver);

//        List<AddAreaDto> seatList = new ArrayList<>();
//        seatList.add(AddAreaDto.builder().areaName("搖滾 A 區").areaPrice("4800").qty("20").build());
//        seatList.add(AddAreaDto.builder().areaName("看台 B 區").areaPrice("3200").qty("50").build());
//        seatList.add(AddAreaDto.builder().areaName("體驗 C 區").areaPrice("800").qty("50").build());

        List<AddAreaDto> seatList = AddAreaDto.createValidDto();

        // 2. 迴圈動態建立與填寫
        for (int i = 0; i < seatList.size(); i++) {
            // 如果不是第一筆(i=0)，就需要點擊「新增」按鈕來展開新的一行輸入框
            if (i > 0) {
                seatPage.clickAddSeatButton();
                // 💡 關鍵提示：AntD 動態表單展開需要幾毫秒的渲染時間，跑太快容易找不到元素。
                // 實務上建議在此稍微等待，或者使用 WebDriverWait
                Thread.sleep(500);
            }
            // 呼叫優化後的方法，一次填滿該行的三個欄位
            seatPage.fillSeatRow(i, seatList.get(i));
        }
        // 3. 填寫完畢，順利進入下一步
        seatPage.clickSubmitButton();
    }

//    @BeforeEach //表示每個測試開始前執行
//    public void loginBeforeEachTest() throws InterruptedException {
//        // 每個 @Test 執行前，都會自動跑這裡，確保永遠在登入狀態
//        webDriver.get("http://localhost:5173/login");
//        LoginPage loginPage = new LoginPage(webDriver);
//        loginPage.login("autoTestUser", "autoTestUser");
//
//        //給瀏覽器 1 到 1.5 秒的時間完成登入後的自動轉址與 Token 寫入
//        Thread.sleep(1500);
//    }

    @Test
    public void fullAddEvent() throws InterruptedException {
        webDriver.get("http://localhost:5173/admin/event-add");

        // 2. 實例化 Page Object
        AddEventPage addEventPage = new AddEventPage(webDriver);

        //上傳圖片
        // 準備你要上傳的圖片絕對路徑 (請根據你的電腦環境修改路徑)
        // Windows 範例: "C:\\tests\\resources\\cover.png"
        // Mac/Linux 範例: "/Users/yourname/tests/resources/cover.png"
        String imgFilePath = "C:\\Users\\2400148\\Pictures\\Screenshots\\test圖片.png";

        AddEventPageDto dto = AddEventPageDto.builder()
                .eventName("自動化測試")
                .fileUploadInput(imgFilePath)
                .eventDetail("做自動化測試ING做自動化測試ING做自動化測試ING做自動化測試ING做自動化測試ING做自動化測試ING")
                .host("自動化測試進行中")
                .eventsLocation("大湖河邊公園旁邊的馬路十字路口")
                .eventDate("2026/06/01 19:00:00")
                .shelfTimeDate("2026/05/24 19:00:00")
                .offTimeDate("2026/06/01 00:00:00")
                .build();
        addEventPage.AddEventPagTest(dto);

        //下方新增Area
        AddAreaPage seatPage = new AddAreaPage(webDriver);

        // 1. 準備多組測試資料 (利用 Builder 模式，非常易讀)
        List<AddAreaDto> seatList = new ArrayList<>();
        seatList.add(AddAreaDto.builder().areaName("搖滾 A 區").areaPrice("4800").qty("20").build());
        seatList.add(AddAreaDto.builder().areaName("看台 B 區").areaPrice("3200").qty("50").build());
        seatList.add(AddAreaDto.builder().areaName("體驗 C 區").areaPrice("800").qty("50").build());

        // 2. 迴圈動態建立與填寫
        for (int i = 0; i < seatList.size(); i++) {
            // 如果不是第一筆(i=0)，就需要點擊「新增」按鈕來展開新的一行輸入框
            if (i > 0) {
                seatPage.clickAddSeatButton();
                // 💡 關鍵提示：AntD 動態表單展開需要幾毫秒的渲染時間，跑太快容易找不到元素。
                // 實務上建議在此稍微等待，或者使用 WebDriverWait
                Thread.sleep(500);
            }
            // 呼叫優化後的方法，一次填滿該行的三個欄位
            seatPage.fillSeatRow(i, seatList.get(i));
        }
        // 3. 填寫完畢，順利進入下一步
        seatPage.clickSubmitButton();
    }

    // ... 在測試方法最後或是 @AfterEach 中呼叫：
    public void captureScreen(String testName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        //幫webDriver 的畫面拍照
        File srcFile = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE);
        try {
            //沒有寫死路徑下,java預設是相對路徑
            //createDirectories 比舊版的 File.mkdir(),自動檢查位置是否有對應的資料夾,沒有才會自動建立
            Files.createDirectories(Paths.get("screenshots"));
            //搬運工:把圖片放入path路徑
            Files.copy(srcFile.toPath(), Paths.get("screenshots/" + testName + "_" + timestamp + ".png"));
            System.out.println("📸 已產生畫面截圖：screenshots/" + testName + "_" + timestamp + ".png");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

