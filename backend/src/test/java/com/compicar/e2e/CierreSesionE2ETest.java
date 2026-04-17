package com.compicar.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

class CierreSesionE2ETest extends BaseE2ETest {

    private static final String SEEDED_LOGIN_EMAIL = "selenium@compicar.test";
    private static final String SEEDED_LOGIN_PASSWORD = "Selenium123!";

    @Test
    void cerrarSesionCierraTokenYredirigeAlHome() {
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
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", loginButton);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loginButton);

        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/perfil"),
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='perfil-page']"))
        ));

        WebElement logoutButton = wait.until(
            ExpectedConditions.elementToBeClickable(By.xpath("//button[normalize-space()='Cerrar sesión']"))
        );
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", logoutButton);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", logoutButton);

        WebElement confirmButton = wait.until(
            ExpectedConditions.elementToBeClickable(By.xpath("//button[normalize-space()='Sí, cerrar sesión']"))
        );
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", confirmButton);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", confirmButton);

        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlToBe(baseUrl + "/"),
            ExpectedConditions.urlContains("/inicio-sesion")
        ));

        String token = (String) ((JavascriptExecutor) driver).executeScript("return window.localStorage.getItem('token');");
        assertTrue(token == null || token.isBlank(), "Se esperaba que el token de sesión se eliminara tras cerrar sesión");

        assertTrue(
            driver.getCurrentUrl().equals(baseUrl + "/") || driver.getCurrentUrl().contains("/inicio-sesion"),
            "Se esperaba redirección a la página principal o login tras cerrar sesión"
        );
    }
}
