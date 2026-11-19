package com.magnetixdian.application;

/**
 * Reglas mínimas de contraseña: 8 caracteres, al menos una letra y un dígito.
 */
public final class PoliticaPassword {

    private PoliticaPassword() {
    }

    public static void validar(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
        boolean letra = password.chars().anyMatch(Character::isLetter);
        boolean digito = password.chars().anyMatch(Character::isDigit);
        if (!letra || !digito) {
            throw new IllegalArgumentException("La contraseña debe incluir al menos una letra y un número.");
        }
    }
}
