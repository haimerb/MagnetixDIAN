package com.magnetixdian.application;

import com.magnetixdian.interfaces.dto.AdquirenteRequest;
import com.magnetixdian.interfaces.dto.LineaFacturaRequest;
import com.magnetixdian.interfaces.dto.NominaElectronicaDto;
import com.magnetixdian.interfaces.dto.ResultadoDocumentoDianDto;
import com.magnetixdian.interfaces.dto.RetencionDianDto;
import com.magnetixdian.interfaces.dto.SolicitudEmisionDianDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServicioNominaElectronica {

    private final ServicioFacturacion facturacion;

    public ServicioNominaElectronica(ServicioFacturacion facturacion) {
        this.facturacion = facturacion;
    }

    public ResultadoDocumentoDianDto emitir(Long empresaId, Long usuarioId, NominaElectronicaDto nomina) {
        if (nomina == null) {
            throw new IllegalArgumentException("La nómina electrónica es obligatoria.");
        }
        if (nomina.trabajadorNumeroDocumento() == null || nomina.trabajadorNumeroDocumento().isBlank()) {
            throw new IllegalArgumentException("La identificación del trabajador es obligatoria.");
        }
        if (nomina.devengados() == null || nomina.devengados().isEmpty()) {
            throw new IllegalArgumentException("La nómina debe tener al menos un devengado.");
        }

        List<LineaFacturaRequest> lineas = new ArrayList<>();
        for (NominaElectronicaDto.ConceptoDto concepto : nomina.devengados()) {
            BigDecimal valor = concepto.valor() == null ? BigDecimal.ZERO : concepto.valor();
            lineas.add(new LineaFacturaRequest(concepto.codigo(), concepto.descripcion(), BigDecimal.ONE, "94",
                    valor, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null));
        }
        List<RetencionDianDto> retenciones = new ArrayList<>();
        if (nomina.deducciones() != null) {
            for (NominaElectronicaDto.ConceptoDto concepto : nomina.deducciones()) {
                BigDecimal valor = concepto.valor() == null ? BigDecimal.ZERO : concepto.valor();
                retenciones.add(new RetencionDianDto("05", "Deducción de nómina", valor, null, valor));
            }
        }
        AdquirenteRequest trabajador = new AdquirenteRequest(
                nomina.trabajadorTipoDocumento() == null || nomina.trabajadorTipoDocumento().isBlank()
                        ? "13" : nomina.trabajadorTipoDocumento(),
                nomina.trabajadorNumeroDocumento().trim(),
                null,
                nomina.trabajadorNombre(),
                null,
                null,
                null,
                "ORDINARIO",
                "R-99-PN");
        String observaciones = "Nómina electrónica - periodo " + nvl(nomina.periodo())
                + " - trabajador " + nvl(nomina.trabajadorNombre());
        SolicitudEmisionDianDto emision = new SolicitudEmisionDianDto(
                "31",
                trabajador,
                lineas,
                retenciones,
                observaciones,
                "1",
                "10",
                "10",
                nomina.fechaPago(),
                null,
                null,
                observaciones,
                nomina);
        return ResultadoDocumentoDianDto.desde(facturacion.emitirSolicitud(empresaId, usuarioId, emision));
    }

    private String nvl(String valor) {
        return valor == null ? "" : valor;
    }
}
