package com.concer.backend.Selenium.Page;

import com.concer.backend.Selenium.Dto.AddAreaDto;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class AddAreaPage {
    private WebDriver driver;

    public AddAreaPage(WebDriver driver) {
        this.driver = driver;
        PageFactory.initElements(driver, this);
    }
    // ✨ 固定的按鈕可以用 @FindBy：假設畫面下方有一個「新增欄位」或「＋」的按鈕
    @FindBy(xpath = "//button[contains(., '新增座位資訊')]")
    private WebElement addSeatButton;
    @FindBy(xpath = "//button[@type='submit'][contains(., '確')]")
    private WebElement submitButton;
    public void clickAddSeatButton() {
        addSeatButton.click();
    }

    public void clickSubmitButton() {
        // 建立 5 秒的等待機制 (假設 driver 已經傳入或存在於類別中)
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

        // 等待直到該 submitButton 在畫面上顯示且可以被點擊
        wait.until(ExpectedConditions.elementToBeClickable(submitButton));
        submitButton.click();
    }
    /**
     * ✨ 完整填寫某一列座位資訊的方法
     * @param index 第幾筆資料 (0, 1, 2...)
     * @param dto 該列的完整資料 (包含名稱、價格、數量)
     */
    public void fillSeatRow(int index, AddAreaDto dto) {
        // 1. 動態組裝三個欄位的 ID
        String nameId = "form_item_listOfControl_" + index + "_areaName";
        String priceId = "form_item_listOfControl_" + index + "_areaPrice";
        String qtyId = "form_item_listOfControl_" + index + "_qty";

        // 2. 抓取並填入「座位名稱」
        WebElement nameInput = driver.findElement(By.id(nameId));
        nameInput.clear();
        nameInput.sendKeys(dto.getAreaName());

        // 3. 抓取並填入「價格」
        WebElement priceInput = driver.findElement(By.id(priceId));
        priceInput.clear();
        priceInput.sendKeys(dto.getAreaPrice());

        // 4. 抓取並填入「座位數量」
        WebElement qtyInput = driver.findElement(By.id(qtyId));
        qtyInput.clear();
        qtyInput.sendKeys(dto.getQty());
    }


}
