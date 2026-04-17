package com.compicar.e2e;

import java.time.Duration;
import java.util.Locale;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

class RegistroE2ETest extends BaseE2ETest {

    @Test
    void registroCreaCuentaYredireccionaAPerfil() {
        String uniqueEmail = String.format(Locale.ROOT, "selenium-reg-%d@compicar.test", System.currentTimeMillis());
        String uniquePhone = String.format(Locale.ROOT, "+34%09d", Math.floorMod(System.currentTimeMillis(), 1_000_000_000L));

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
        telefonoInput.sendKeys(uniquePhone);
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

        WebDriverWait postSubmitWait =
        new WebDriverWait(driver, Duration.ofSeconds(20));

        By profileButtonBy = By.xpath("//button[normalize-space()='Ir a mi perfil']");
        By anyErrorBy = By.cssSelector("div.border-red-200, p.text-red-600");

        postSubmitWait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/perfil"),
            ExpectedConditions.visibilityOfElementLocated(profileButtonBy),
            ExpectedConditions.visibilityOfElementLocated(anyErrorBy)
        ));

        if (!driver.findElements(anyErrorBy).isEmpty()) {
            Assertions.fail(
                "Registro no llegó a éxito. Mensaje UI: " + driver.findElement(anyErrorBy).getText()
            );
        }

        if (!driver.getCurrentUrl().contains("/perfil")) {
            WebElement profileButton = postSubmitWait.until(
                ExpectedConditions.elementToBeClickable(profileButtonBy)
            );
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", profileButton);
        }

        wait.until(ExpectedConditions.or(
            ExpectedConditions.urlContains("/perfil"),
            ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='perfil-page']"))
        ));
    }
}
