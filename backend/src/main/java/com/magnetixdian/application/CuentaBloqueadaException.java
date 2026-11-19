package com.magnetixdian.application;

/**
 * La cuenta está bloqueada temporalmente por intentos fallidos consecutivos.
 */
public class CuentaBloqueadaException extends RuntimeException {

    public CuentaBloqueadaException(String message) {
        super(message);
    }
}
