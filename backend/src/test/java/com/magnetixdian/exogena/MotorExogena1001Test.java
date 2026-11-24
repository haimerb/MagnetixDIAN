package com.magnetixdian.exogena;

import com.magnetixdian.domain.exogena.AgregadorExogena;
import com.magnetixdian.domain.exogena.ClasificadorMovimientos;
import com.magnetixdian.domain.exogena.FormatoTributarioCatalogo;
import com.magnetixdian.domain.exogena.GeneradorExcelExogena;
import com.magnetixdian.domain.exogena.GeneradorXml1001Exogena;
import com.magnetixdian.domain.exogena.MapeadorCuentaConcepto;
import com.magnetixdian.domain.exogena.ValidadorExogena;
import com.magnetixdian.domain.exogena.ValidadorXsdExogena;
import com.magnetixdian.domain.rule.NitDigitoVerificacionRegla;
import com.magnetixdian.interfaces.dto.DatosDeclaranteDto;
import com.magnetixdian.interfaces.dto.DianConceptoDto;
import com.magnetixdian.interfaces.dto.ExogenaRenglonDto;
import com.magnetixdian.interfaces.dto.ExogenaValidacionDto;
import com.magnetixdian.interfaces.dto.MovimientoTributarioDto;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotorExogena1001Test {

    private static final int ANO = 2026;
    private static final String NIT = "900123456";

    private final ClasificadorMovimientos clasificador = new ClasificadorMovimientos();
    private final AgregadorExogena agregador = new AgregadorExogena();
    private final ValidadorExogena validador = new ValidadorExogena();

    @Test
    void clasificadorInfiereConceptoDesdeCuenta() {
        MovimientoTributarioDto origen = MovimientoTributarioDto.deAsiento(
                1L, 10L, 11L, 2L, "NIT", NIT, "DEMO SAS", "413505", null,
                new BigDecimal("100.00"), LocalDate.of(ANO, 3, 15), "FACTURA", "F-1", "FACTURACION");

        MovimientoTributarioDto clasificado = clasificador.clasificar(List.of(origen)).get(0);

        assertEquals("1001", clasificado.conceptoCodigo());
        assertEquals(ANO, clasificado.ano());
        assertEquals("CO", clasificado.pais());
        assertEquals("FACTURACION", clasificado.origen());
    }

    @Test
    void mapeadorPrefiereCoincidenciaExactaYPrefijoMasLargo() {
        MapeadorCuentaConcepto mapeador = new MapeadorCuentaConcepto();
        Map<String, Long> cuentas = Map.of("413", 1L, "413505", 10L);
        Map<String, Long> conceptos = Map.of("1001", 1L, "1002", 10L);

        assertEquals(10L, mapeador.mapear("413505", cuentas, conceptos).orElseThrow());
        assertEquals(1L, mapeador.mapear("413599", cuentas, conceptos).orElseThrow());
        assertTrue(mapeador.mapear("999999", cuentas, conceptos).isEmpty());
    }

    @Test
    void agregadorSumaPorTerceroYConcepto() {
        DianConceptoDto concepto = concepto();
        List<ExogenaRenglonDto> renglones = agregador.agregar(FormatoTributarioCatalogo.FORMATO_1001,
                List.of(movimientoValido(new BigDecimal("100.00")),
                        movimientoValido(new BigDecimal("50.00"))), Map.of(10L, concepto));

        assertEquals(1, renglones.size());
        assertEquals(0, new BigDecimal("150.00").compareTo(renglones.get(0).valor()));
        assertEquals(2, renglones.get(0).cantidadMovimientos());
    }

    @Test
    void validadorBloqueaDvInvalidoYPasaConDvCorrecto() {
        DianConceptoDto concepto = concepto();
        List<ExogenaValidacionDto> correctas = validador.validar(ANO, FormatoTributarioCatalogo.FORMATO_1001,
                List.of(movimientoValido(new BigDecimal("100.00"))), Map.of(10L, concepto), Set.of("413505"));

        assertFalse(validador.hayBloqueantes(correctas));

        MovimientoTributarioDto invalido = new MovimientoTributarioDto(1L, ANO, 10L, 11L, 2L,
                "NIT", NIT, "99", "DEMO SAS", "BOGOTA", "CO", "413505", "1001", 10L,
                new BigDecimal("100.00"), LocalDate.of(ANO, 3, 15), "FACTURA", "F-1", "FACTURACION");
        List<ExogenaValidacionDto> conError = validador.validar(ANO, FormatoTributarioCatalogo.FORMATO_1001,
                List.of(invalido), Map.of(10L, concepto), Set.of("413505"));

        assertTrue(validador.hayBloqueantes(conError));
        assertTrue(conError.stream().anyMatch(v -> "DV_INVALIDO".equals(v.codigo())));
    }

    @Test
    void xml1001EsValidoConXsdYExcelSeGenera() throws Exception {
        DianConceptoDto concepto = concepto();
        ExogenaRenglonDto renglon = agregador.agregar(FormatoTributarioCatalogo.FORMATO_1001,
                List.of(movimientoValido(new BigDecimal("150.00"))), Map.of(10L, concepto)).get(0);
        DatosDeclaranteDto declarante = new DatosDeclaranteDto(NIT, dv(), "DEMO SAS", "NIT",
                "BOGOTA", "CO", "NIIF_PYMES", "2025");

        String xml = new GeneradorXml1001Exogena().generar(declarante, List.of(renglon));
        new ValidadorXsdExogena().validar(ANO, FormatoTributarioCatalogo.FORMATO_1001, xml);

        assertTrue(xml.contains("prevalidacionOficial=\"false\""));
        assertTrue(xml.contains("formato=\"1001\""));

        byte[] excel = new GeneradorExcelExogena().generar(FormatoTributarioCatalogo.FORMATO_1001,
                List.of(renglon));
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
            assertEquals("Formato 1001", libro.getSheetName(0));
            assertEquals("CONCEPTO", libro.getSheetAt(0).getRow(0).getCell(9).getStringCellValue());
        }
    }

    @Test
    void xsdNoDisponibleParaFormatoRegistrado() {
        String xml = new GeneradorXml1001Exogena().generar(null, List.of());
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new ValidadorXsdExogena().validar(ANO, "1003", xml));

        assertTrue(error.getMessage().contains("Ruta esperada: /xsd/exogena/2026/1003.xsd"));
    }

    private static DianConceptoDto concepto() {
        return new DianConceptoDto(10L, ANO, FormatoTributarioCatalogo.FORMATO_1001, "1001",
                "Ingresos operacionales", "JURIDICA", "NIT", BigDecimal.ZERO, true);
    }

    private static MovimientoTributarioDto movimientoValido(BigDecimal valor) {
        return new MovimientoTributarioDto(1L, ANO, 10L, 11L, 2L, "NIT", NIT, dv(),
                "DEMO SAS", "BOGOTA", "CO", "413505", "1001", 10L, valor,
                LocalDate.of(ANO, 3, 15), "FACTURA", "F-1", "FACTURACION");
    }

    private static String dv() {
        return String.valueOf(NitDigitoVerificacionRegla.calcularDv(NIT));
    }
}
