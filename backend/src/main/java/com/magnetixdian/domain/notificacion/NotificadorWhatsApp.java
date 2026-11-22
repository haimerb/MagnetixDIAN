package com.magnetixdian.domain.notificacion;

public interface NotificadorWhatsApp {

    String normalizarNumero(String numero);

    boolean numeroValido(String numero);

    String construirEnlace(String numero, String mensaje);
}
