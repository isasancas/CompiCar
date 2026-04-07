package com.compicar.e2e;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

class RegistroE2ETest extends BaseE2ETest {

    @Test
    void registroCreaCuentaYredireccionaAPerfil() {
        String uniqueEmail = String.format(Locale.ROOT, "selenium-reg-%d@compicar.test", System.currentTimeMillis());

        driver.get(baseUrl + "/registro");

        WebElement nombreInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("nombre"))
        );
        WebElement primerApellidoInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("primerApellido"))
        );
        WebElement telefonoInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("telefono"))
        );
        WebElement emailInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("email"))
        );
        WebElement passwordInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("password"))
        );
        WebElement confirmPasswordInput = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("confirmPassword"))
        );
        WebElement termsCheckbox = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='checkbox']"))
        );
        WebElement submitButton = wait.until(
            ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']"))
        );

        nombreInput.clear();
        nombreInput.sendKeys("Selenium");
        primerApellidoInput.clear();
        primerApellidoInput.sendKeys("Test");
        telefonoInput.clear();
        telefonoInput.sendKeys("+34123456789");
        emailInput.clear();
        emailInput.sendKeys(uniqueEmail);
        passwordInput.clear();
        passwordInput.sendKeys("Selenium123!");
        confirmPasswordInput.clear();
        confirmPasswordInput.sendKeys("Selenium123!");
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", termsCheckbox);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", termsCheckbox);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", submitButton);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", submitButton);

        WebElement profileButton = wait.until(
            ExpectedConditions.elementToBeClickable(By.xpath("//button[normalize-space()='Ir a mi perfil']"))
        );
        profileButton.click();

        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/perfil"),
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='perfil-page']"))
        ));

        assertTrue(
            driver.getCurrentUrl().contains("/perfil"),
            "Se esperaba navegación a /perfil tras el registro"
        );
    }
}
