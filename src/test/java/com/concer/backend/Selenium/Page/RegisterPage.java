package com.concer.backend.Selenium.Page;
import com.concer.backend.Selenium.Dto.RegisterPageDto;
import lombok.Getter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;


import java.time.Duration;

public class RegisterPage {

    private WebDriver driver;
    private WebDriverWait wait; // ✨ 新增等待物件
    // 修正：將 contains(text(), ...) 改為 contains(@placeholder, ...)
    @FindBy(xpath = "//input[contains(@placeholder, '帳號')]")
    private WebElement accountInput;

    @FindBy(xpath = "//input[contains(@placeholder, '密碼') and not(contains(@placeholder, '確認'))]")
    private WebElement passwordInput;

    @FindBy(xpath = "//input[contains(@placeholder, '確認密碼')]")
    private WebElement rePasswordInput;

    @FindBy(xpath = "//input[contains(@placeholder, '暱稱')]")
    private WebElement nickNameInput;

    // 修正：補上後面漏掉的單引號 '
    @FindBy(xpath = "//input[contains(@placeholder, 'Email')]")
    private WebElement emailInput;

    // 修正：補上後面漏掉的單引號 '
    @FindBy(xpath = "//input[contains(@placeholder, '手機')]")
    private WebElement phoneInput;

    // 2. 提供給 register 方法使用的 Getter
    // 補完：使用 class 定位該下拉選單（也可以用 //select[@class='custom-select']）
    @Getter
    @FindBy(className = "custom-select")
    private WebElement statusInput;

    // 按鈕的文字確實包裹在標籤內，所以用 text() 是正確的
    @FindBy(xpath = "//button[contains(text(), '送出')]")
    private WebElement registerButton;

    //用於抓取錯誤訊息
    @FindBy(className = "error-msg")
    private WebElement errorMessage;

    public String getErrorMessageText() {
        return errorMessage.getText();
    }


    // 建構子
    public RegisterPage(WebDriver driver) {
        this.driver = driver;
        // 設定最長等待 10 秒
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }
    /**
     * 執行註冊的方法
     * @param account 帳號
     * @param password 密碼
     * @param rePassword 確認密碼
     * @param nickName 暱稱
     * @param email 信箱
     * @param phone 手機
     * @param status 身分別 1管理員,0一般會員
     */
    public void register( String account, String password ,String rePassword ,String nickName, String email ,String phone ,String status){
        // ✨ ✨ 關鍵防禦：在 clear() 之前，強制等待「帳號欄位」出現在畫面上
        wait.until(ExpectedConditions.visibilityOf(accountInput));

        accountInput.clear();
        accountInput.sendKeys(account);

        passwordInput.sendKeys(password);

        rePasswordInput.sendKeys(rePassword);

        nickNameInput.sendKeys(nickName);

        emailInput.sendKeys(email);

        phoneInput.sendKeys(phone);

        Select statusSelect = new Select(getStatusInput());
        statusSelect.selectByValue(status);

        registerButton.click();

    }
    /**
     * 執行註冊的方法ByDto
     * @param registerPageDto
     */
    public void registerByDto(RegisterPageDto registerPageDto){
        // ✨ ✨ 關鍵防禦：在 clear() 之前，強制等待「帳號欄位」出現在畫面上
        wait.until(ExpectedConditions.visibilityOf(accountInput));


        accountInput.clear();
        accountInput.sendKeys(registerPageDto.getAccountInput());

        passwordInput.clear();
        passwordInput.sendKeys(registerPageDto.getPasswordInput());

        rePasswordInput.clear();
        rePasswordInput.sendKeys(registerPageDto.getRePasswordInput());

        nickNameInput.clear();
        nickNameInput.sendKeys(registerPageDto.getNickNameInput());

        emailInput.clear();
        emailInput.sendKeys(registerPageDto.getEmailInput());

        phoneInput.clear();
        phoneInput.sendKeys(registerPageDto.getPhoneInput());

        Select statusSelect = new Select(getStatusInput());
        statusSelect.selectByValue(registerPageDto.getStatus());

        registerButton.click();
    }

}

