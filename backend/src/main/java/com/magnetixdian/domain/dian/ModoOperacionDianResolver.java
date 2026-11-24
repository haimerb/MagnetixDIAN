package com.magnetixdian.domain.dian;

import org.springframework.stereotype.Component;

@Component
public class ModoOperacionDianResolver {

    public boolean esTenantInterno(ModoOperacionDian modo) {
        return validar(modo) == ModoOperacionDian.PROPIO_INTERNO;
    }

    public boolean permiteFacturarATerceros(ModoOperacionDian modo) {
        return switch (validar(modo)) {
            case PT_MAGNETIXDIAN, PT_EXTERNO -> true;
            case PROPIO_INTERNO, ADQUIRIDO_CLIENTE -> false;
        };
    }

    public boolean requiereHabilitacionProveedorTecnologico(ModoOperacionDian modo) {
        return switch (validar(modo)) {
            case PT_MAGNETIXDIAN, PT_EXTERNO -> true;
            case PROPIO_INTERNO, ADQUIRIDO_CLIENTE -> false;
        };
    }

    public boolean esAdquirido(ModoOperacionDian modo) {
        return validar(modo) == ModoOperacionDian.ADQUIRIDO_CLIENTE;
    }

    public ModoOperacionDian modoPorDefectoParaCliente() {
        return ModoOperacionDian.ADQUIRIDO_CLIENTE;
    }

    public ModoOperacionDian resolver(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("Modo DIAN no soportado: null");
        }
        return switch (valor) {
            case "PROPIO_INTERNO" -> ModoOperacionDian.PROPIO_INTERNO;
            case "ADQUIRIDO_CLIENTE" -> ModoOperacionDian.ADQUIRIDO_CLIENTE;
            case "PT_MAGNETIXDIAN" -> ModoOperacionDian.PT_MAGNETIXDIAN;
            case "PT_EXTERNO" -> ModoOperacionDian.PT_EXTERNO;
            default -> throw new IllegalArgumentException("Modo DIAN no soportado: " + valor);
        };
    }

    private ModoOperacionDian validar(ModoOperacionDian modo) {
        if (modo == null) {
            throw new IllegalArgumentException("Modo DIAN no soportado: null");
        }
        return modo;
    }
}
