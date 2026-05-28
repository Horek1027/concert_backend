package com.concer.backend.Selenium;


import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebDriverConfig {
    @Bean
    // 不在這裡加上 @Scope("prototype")
    //Spring 預設就是 Singleton (單例)，這會確保整個專案只會執行一次 new ChromeDriver()
    public WebDriver webDriver() {
        ChromeOptions options = new ChromeOptions();

        options.addArguments("--remote-allow-origins=*");

        // 測試結束、Driver session 結束後，不自動關閉 Chrome 視窗
        options.setExperimentalOption("detach", true);

        // 建立唯一的瀏覽器實例
        WebDriver driver = new ChromeDriver(options);

        // 將視窗移到上方螢幕
        // 如果沒有移到正確螢幕，可以把 -1080 改成 -1000、-1440 等符合你螢幕高度的值
//        driver.manage().window().setPosition(new Point(0, -1080));

        // 在上方螢幕最大化
        driver.manage().window().maximize();

        return driver;
    }
}

