package com.magnetixdian.infrastructure.xml;

import com.magnetixdian.domain.factura.EventoDian;
import com.magnetixdian.infrastructure.persistence.EmpresaJpa;
import com.magnetixdian.infrastructure.persistence.FacturaJpa;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorApplicationResponseTest {

    private final GeneradorApplicationResponse generador = new GeneradorApplicationResponse();

    private FacturaJpa factura() {
        EmpresaJpa empresa = new EmpresaJpa("900123456", "MAGNETIXDIAN SAS");
        FacturaJpa f = new FacturaJpa();
        f.setEmpresa(empresa);
        f.setNumeroCompleto("FV-100");
        f.setAdquirenteTipoDoc("31");
        f.setAdquirenteNumDoc("830053812");
        f.setAdquirenteNombre("Cliente SAS");
        return f;
    }

    @Test
    void generaApplicationResponseConCodigoYReferencia() {
        String xml = generador.generar(factura(), EventoDian.ACEPTACION_EXPRESA,
                "cufe-abc", "cude-evento", "FV-100-033", OffsetDateTime.now(),
                "sw-1", "sec-1", null);

        assertThat(xml).contains("<cbc:ResponseCode>033</cbc:ResponseCode>");
        assertThat(xml).contains("cufe-abc");
        assertThat(xml).contains("cude-evento");
        assertThat(xml).contains("CUDE-SHA384");
        assertThat(xml).contains("FV-100");
        assertThat(xml).contains("ApplicationResponse");
    }
}
