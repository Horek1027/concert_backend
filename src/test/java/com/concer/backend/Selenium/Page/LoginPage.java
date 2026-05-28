package com.concer.backend.Selenium.Page;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LoginPage {
    private WebDriver driver;
    private WebDriverWait wait; // 抽成類別變數，讓其他方法可以用

    // 使用 Selenium 的 PageFactory 簡化元件初始化
    @FindBy(css = "input[type='account']")
    private WebElement accountInput;

    @FindBy(css = "input[type='password']")
    private WebElement passwordInput;

    @FindBy(xpath = "//button[contains(text(), '登入')]")
    private WebElement loginButton;

    // 建構子
    public LoginPage(WebDriver driver) {
        this.driver = driver;
        // 1. 初始化等待物件（最多等 10 秒）
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        // 2. 初始化元件
        PageFactory.initElements(driver, this);
    }

    /**
     * 執行登入流程的方法
     * @param account 帳號
     * @param password 密碼
     */
    public void login(String account, String password) {

        // ✨ ✨ 關鍵防禦：在 clear() 之前，強制等待「帳號欄位」出現在畫面上
        wait.until(ExpectedConditions.visibilityOf(accountInput));
        // 先清除欄位可能殘留的預設文字，再輸入
        accountInput.clear();
        accountInput.sendKeys(account);

        passwordInput.clear();
        passwordInput.sendKeys(password);

        // 點擊登入按鈕
        loginButton.click();
    }
}
