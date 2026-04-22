package com.compicar.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

class LoginE2ETest extends BaseE2ETest {

    private static final String SEEDED_LOGIN_EMAIL = "selenium@compicar.test";
    private static final String SEEDED_LOGIN_PASSWORD = "Selenium123!";

    @Test
    void loginRedirigeAPerfilConCredencialesValidas() {
        driver.get(baseUrl + "/inicio-sesion");

        WebElement emailInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='input-email']"))
        );
        WebElement passwordInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='input-password']"))
        );
        WebElement loginButton = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector("[data-testid='btn-login']"))
        );

        emailInput.clear();
        emailInput.sendKeys(SEEDED_LOGIN_EMAIL);
        passwordInput.clear();
        passwordInput.sendKeys(SEEDED_LOGIN_PASSWORD);
        loginButton.click();

        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/perfil"),
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='perfil-page']"))
        ));

        assertTrue(
            driver.getCurrentUrl().contains("/perfil"),
            "Se esperaba navegación a /perfil tras el login"
        );
    }
}
