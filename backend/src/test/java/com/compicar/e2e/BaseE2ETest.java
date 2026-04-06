package com.compicar.e2e;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

public abstract class BaseE2ETest {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected String baseUrl;

    @BeforeEach
    void setUpDriver() {
        driver = createDriver();

        long timeoutSeconds = getLongEnv("E2E_TIMEOUT_SECONDS", 12L);
        wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        baseUrl = System.getenv().getOrDefault("E2E_BASE_URL", "http://localhost:5173");
    }

    @AfterEach
    void tearDownDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    private WebDriver createDriver() {
        String browser = System.getenv().getOrDefault("E2E_BROWSER", "chrome").trim().toLowerCase();
        if ("edge".equals(browser)) {
            return createEdgeDriver();
        }
        if ("chrome".equals(browser)) {
            try {
                return createChromeDriver();
            } catch (WebDriverException ex) {
                if (isChromeBinaryMissing(ex)) {
                    return createEdgeDriver();
                }
                throw ex;
            }
        }

        throw new IllegalArgumentException("Unsupported E2E_BROWSER value: " + browser + ". Use 'chrome' or 'edge'.");
    }

    private WebDriver createChromeDriver() {
        applyDriverPath("webdriver.chrome.driver", "E2E_CHROME_DRIVER_PATH");

        ChromeOptions options = new ChromeOptions();
        if (isHeadlessEnabled()) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1440,900");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");

        try {
            return new ChromeDriver(options);
        } catch (WebDriverException ex) {
            throw enrichDriverError(ex, "chrome", "E2E_CHROME_DRIVER_PATH", "webdriver.chrome.driver");
        }
    }

    private WebDriver createEdgeDriver() {
        applyDriverPath("webdriver.edge.driver", "E2E_EDGE_DRIVER_PATH");

        EdgeOptions options = new EdgeOptions();
        if (isHeadlessEnabled()) {
            options.addArguments("--headless=new");
        }
        options.addArguments("--window-size=1440,900");
        options.addArguments("--disable-gpu");

        try {
            return new EdgeDriver(options);
        } catch (WebDriverException ex) {
            throw enrichDriverError(ex, "edge", "E2E_EDGE_DRIVER_PATH", "webdriver.edge.driver");
        }
    }

    private void applyDriverPath(String systemProperty, String envName) {
        String envPath = System.getenv(envName);
        if (envPath != null && !envPath.isBlank()) {
            System.setProperty(systemProperty, envPath);
        }
    }

    private RuntimeException enrichDriverError(
        WebDriverException ex,
        String browser,
        String envName,
        String systemProperty
    ) {
        String message = "No se pudo iniciar Selenium con " + browser + ". "
            + "Si no tienes internet o el driver no esta en cache, define " + envName
            + " o la propiedad " + systemProperty + " con la ruta al ejecutable del driver.";
        return new IllegalStateException(message, ex);
    }

    private boolean isChromeBinaryMissing(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("cannot find chrome binary")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isHeadlessEnabled() {
        String explicit = System.getenv("E2E_HEADLESS");
        if (explicit != null) {
            return Boolean.parseBoolean(explicit);
        }
        return System.getenv("CI") != null;
    }

    private long getLongEnv(String key, long defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
