package com.concer.backend.Selenium.Page;

import com.concer.backend.Selenium.Dto.AddEventPageDto;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Data
@Slf4j
public class AddEventPage {
    private WebDriver driver;
    private WebDriverWait wait;

    @FindBy(xpath = "//input[@type='file' and contains(@accept, 'image')]")
    private WebElement fileUploadInput;

    @FindBy(xpath = "//input[@id='form_item_eventsName']")
    private WebElement eventNameInput;

    @FindBy(xpath = "//input[@id='form_item_eventsOrganizer']")
    private WebElement hostInput;

    @FindBy(xpath = "//input[@id='form_item_eventsLocation']")
    private WebElement eventLocationInput;

    @FindBy(id = "form_item_eventDate")
    private WebElement eventDateInput;

    @FindBy(id = "form_item_shelfTime")
    private WebElement shelfTimeDateInput;

    @FindBy(id = "form_item_offSaleTime")
    private WebElement offTimeDateInput;

    @FindBy(id = "form_item_eventsDetails")
    private WebElement eventDetailInput;

    @FindBy(xpath = "//button[contains(., '下一步')]")
    private WebElement nextButton;

    //前面宣告這邊才是建立物件
    public AddEventPage(WebDriver driver) {
        this.driver = driver;
        // 初始化：設定最長等 10 秒，每 0.5 秒會去網頁檢查一次
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        PageFactory.initElements(driver, this);
    }

    /**
     * 主要方法：自動化設定 Ant Design 的日期時間選擇器 (DatePicker)
     * @param element 網頁上的輸入框元件
     * @param dateStr 想要輸入的日期時間字串 (例如 "2026-05-20 14:30:00")
     */
    private void setDatePicker(WebElement element, String dateStr) throws InterruptedException {
        // 1. 將文字格式的日期，轉換為 Java 的日期物件，方便後續提取年、月、日、時、分、秒
        LocalDateTime target = parseDateTime(dateStr);

        // 2. 確保輸入框目前是處於可以操作的狀態（不是反灰或禁用的）
        WebElement input = waitUntilPickerEnabled(element);

        // 3. 找到包覆在外層的日曆外殼元件 (Ant design Vue的設計通常要把事件綁在外殼)
        WebElement picker = getPickerWrapper(input);

        // 4. 【畫面捲動】使用 JavaScript 將日曆元件捲動到瀏覽器畫面的正中央，避免被其他元件遮擋
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", picker);
        Thread.sleep(300); // 稍微暫停 0.3 秒，等待瀏覽器滾動動畫完成

        // 5. 【打開日曆】模擬滑鼠點擊，把日曆的下拉選單打開
        openDatePicker(input, picker);

        // 6. 【獲取彈出視窗】等待並捕捉到畫面上彈出來的日曆選單視窗 (Dropdown)
        WebElement dropdown = waitForOpenDatePicker(Duration.ofSeconds(2));

        // 7. 【切換月份】比對目前選單的月份，自動點擊「上個月」或「下個月」按鈕，切換到目標月份
        movePickerToMonth(dropdown, YearMonth.from(target));

        // 8. 【選擇日期】將目標日期轉換為標準格式 (例如 "2026-05-20")
        String dateTitle = target.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        // 在選單中尋找符合這個日期、且「沒有被禁用」的格子 (td)
        WebElement dateCell = dropdown.findElement(By.xpath(
                ".//td[@title='" + dateTitle + "' and not(contains(@class,'ant-picker-cell-disabled'))]//div"
        ));
        dateCell.click(); // 點擊該日期格子

        // 9. 【選擇時間】依序點擊時間面板中的：第一欄(時)、第二欄(分)、第三欄(秒)
        selectTimeValueByColumn(dropdown, 1, target.getHour());   // 設定小時
        selectTimeValueByColumn(dropdown, 2, target.getMinute()); // 設定分鐘
        selectTimeValueByColumn(dropdown, 3, target.getSecond()); // 設定秒數

        // 10.【點擊確定】在選單中找到「確定」按鈕並點擊，完成時間選擇
        dropdown.findElement(By.xpath(".//li[contains(@class,'ant-picker-ok')]//button")).click();

        // 11.【驗證結果】安全檢查：等待輸入框裡面確實已經出現了文字，確認剛才的操作有成功寫入
        //wait.until : 等待直到「確定」按鈕可以被點擊，才執行下一步
        wait.until(driver -> {
            String val = input.getAttribute("value");
            return val != null && !val.isBlank();
        });

        // 12.【等待選單消失】確認日曆選單已經在畫面上隱藏、關閉了
        wait.until(ExpectedConditions.invisibilityOf(dropdown));

        // 13.【畫面還原】習慣性將網頁捲動回最底部，避免影響下一個測試步驟的操作
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
        Thread.sleep(500); // 暫停 0.5 秒，確保頁面穩定
    }

    /**
     * 輔助方法：自動切換日曆月份
     * @param dropdown 日曆彈出視窗元件
     * @param targetMonth 目標的年份與月份
     */
    private void movePickerToMonth(WebElement dropdown, YearMonth targetMonth) {
        // 取得當前日曆畫面上顯示的是哪個月份
        YearMonth currentMonth = currentPickerMonth(dropdown);

        // 計算當前月份與目標月份「差幾個月」（例如：現在5月，目標8月，相差 3 個月）
        long monthsDiff = ChronoUnit.MONTHS.between(currentMonth, targetMonth);

        // 如果剛好是同一個月，就不用切換，直接結束這個方法
        if (monthsDiff == 0) return;

        // 判斷要點擊「下一月」還是「上一月」的按鈕類別名稱 (Class)
        String btnClass = monthsDiff > 0 ? "ant-picker-header-next-btn" : "ant-picker-header-prev-btn";
        WebElement button = dropdown.findElement(By.cssSelector("button." + btnClass));

        // 取絕對值（把負數變正數），算好需要點擊幾次，用迴圈連續點擊
        long steps = Math.abs(monthsDiff);
        for (int i = 0; i < steps; i++) {
            button.click();
        }
    }

    /**
     * 輔助方法：在時間滾輪面板中選擇特定的數值
     * @param dropdown 日曆彈出視窗元件
     * @param columnIndex 第幾欄（1=時、2=分、3=秒）
     * @param value 要選擇的數字（例如 14 點）
     */
    private void selectTimeValueByColumn(WebElement dropdown, int columnIndex, int value) {
        // 將數字補零變成兩位數（例如數字 5 會變成字串 "05"）
        String text = String.format("%02d", value);

        // 定位到該欄位中，對應數字的選項
        WebElement option = dropdown.findElement(By.xpath(
                "(.//ul[contains(@class,'ant-picker-time-panel-column')])[" + columnIndex + "]" +
                        "//div[contains(@class,'ant-picker-time-panel-cell-inner') and normalize-space()='" + text + "']"
        ));

        // 因為時間選單很長，必須先用 JS 把該選項滾動到中心可見位置，然後再點擊它
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'}); arguments[0].click();", option);
    }

    /**
     * 輔助方法：嘗試用不同的方式打開日曆（防止網頁元件擋住點擊失敗）
     */
    private void openDatePicker(WebElement input, WebElement picker) {
        try {
            // 嘗試方法 1：直接點擊輸入框本體
            input.click();
        } catch (Exception e) {
            // 如果方法 1 失敗（可能被外殼擋住），嘗試方法 2：移動到外殼元件上再點擊
            new Actions(driver).moveToElement(picker).click().perform();
        }
    }

    /**
     * 輔助方法：等待並確認輸入框是可以操作的狀態
     */
    private WebElement waitUntilPickerEnabled(WebElement input) {
        return wait.until(d -> {
            WebElement picker = getPickerWrapper(input);

            // 檢查各種可能導致元件被禁用的屬性（HTML 的 disabled、aria-disabled 或特定的 class）
            boolean disabled = input.getAttribute("disabled") != null
                    || "true".equals(input.getAttribute("aria-disabled"))
                    || picker.getAttribute("class").contains("ant-picker-disabled");

            // 如果元件顯示在畫面上，而且沒有被禁用，就返回這個元件，否則繼續等待
            return (input.isDisplayed() && !disabled) ? input : null;
        });
    }

    /**
     * 輔助方法：透過子元件（輸入框）向上尋找它的父級日曆外殼
     */
    private WebElement getPickerWrapper(WebElement input) {
        // XPath 的 ancestor:: 表示向上尋找符合條件的祖先節點
        return input.findElement(By.xpath("./ancestor::div[contains(@class,'ant-picker')]"));
    }

    /**
     * 輔助方法：把文字解析成 Java 的時間物件，支援兩種常見格式：斜線「/」或橫線「-」
     */
    private LocalDateTime parseDateTime(String dateStr) {
        String pattern = dateStr.contains("/") ? "yyyy/MM/dd HH:mm:ss" : "yyyy-MM-dd HH:mm:ss";
        return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 輔助方法：動態尋找網頁上已經打開且「顯示中」的日曆彈出選單
     */
    private WebElement waitForOpenDatePicker(Duration timeout) {
        return new WebDriverWait(driver, timeout).until(d -> d.findElements(By.xpath(
                "//div[contains(@class,'ant-picker-dropdown') and not(contains(@class,'ant-picker-dropdown-hidden'))]"
        )).stream().filter(WebElement::isDisplayed).findFirst().orElse(null));
    }

    /**
     * 輔助方法：取得當前日曆畫面上顯示的是哪一個年月份
     */
    private YearMonth currentPickerMonth(WebElement dropdown) {
        // 在目前畫面上，找到隨便一個看得到的日期格子，抓取它的 title 屬性（裡面藏有 yyyy-MM-dd 的資訊）
        String title = dropdown.findElement(By.cssSelector("td.ant-picker-cell-in-view[title]")).getAttribute("title");
        // 將這個日期轉換為年月份物件 (YearMonth) 並返回
        return YearMonth.from(LocalDate.parse(title));
    }






    public void uploadCoverImage(String absoluteFilePath) {
        fileUploadInput.sendKeys(absoluteFilePath);
    }

    public void AddEventPagTest(AddEventPageDto addEventPagDto) throws InterruptedException {
        wait.until(ExpectedConditions.visibilityOf(eventNameInput));

        uploadCoverImage(addEventPagDto.getFileUploadInput());

        eventNameInput.clear();
        eventNameInput.sendKeys(addEventPagDto.getEventName());

        hostInput.clear();
        hostInput.sendKeys(addEventPagDto.getHost());

        eventLocationInput.clear();
        eventLocationInput.sendKeys(addEventPagDto.getEventsLocation());

        setDatePicker(eventDateInput, addEventPagDto.getEventDate());
        setDatePicker(shelfTimeDateInput, addEventPagDto.getShelfTimeDate());
        setDatePicker(offTimeDateInput, addEventPagDto.getOffTimeDate());

        eventDetailInput.clear();
        eventDetailInput.sendKeys(addEventPagDto.getEventDetail());

        nextButton.click();
    }
}
