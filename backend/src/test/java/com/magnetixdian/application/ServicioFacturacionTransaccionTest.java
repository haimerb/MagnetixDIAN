package com.magnetixdian.application;

import com.magnetixdian.interfaces.dto.FacturaRequest;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ServicioFacturacionTransaccionTest {

    @Test
    void emitirDebeSerTransaccionalParaPublicarEventoDespuesDelCommit() throws Exception {
        Method metodo = ServicioFacturacion.class.getMethod(
                "emitir", Long.class, Long.class, FacturaRequest.class);

        assertNotNull(metodo.getAnnotation(Transactional.class));
    }
}
