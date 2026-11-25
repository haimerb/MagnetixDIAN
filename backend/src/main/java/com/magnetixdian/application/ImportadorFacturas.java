package com.magnetixdian.application;

import com.magnetixdian.infrastructure.excel.LectorFilasImportacion;
import com.magnetixdian.interfaces.dto.AdquirenteRequest;
import com.magnetixdian.interfaces.dto.FacturaRequest;
import com.magnetixdian.interfaces.dto.LineaFacturaRequest;
import com.magnetixdian.interfaces.dto.ResultadoEmisionDto;
import com.magnetixdian.interfaces.dto.ResultadoImportacionDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Importa facturas desde un archivo CSV o Excel. Cada fila representa una línea
 * y las filas se agrupan por la columna {@code grupo}: un grupo equivale a un
 * documento electrónico que se emite con el flujo normal de facturación.
 */
@Service
public class ImportadorFacturas {

    private static final int MAX_FILAS = 1000;

    private final ServicioFacturacion facturacion;

    public ImportadorFacturas(ServicioFacturacion facturacion) {
        this.facturacion = facturacion;
    }

    public ResultadoImportacionDto importar(Long empresaId, Long usuarioId, byte[] contenido, String nombreArchivo) {
        return importar(empresaId, usuarioId, contenido, nombreArchivo, null);
    }

    public ResultadoImportacionDto importar(Long empresaId, Long usuarioId, byte[] contenido, String nombreArchivo,
                                            Map<String, Integer> mapeo) {
        List<List<String>> filas = LectorFilasImportacion.leer(contenido, nombreArchivo);
        if (filas.size() < 2) {
            return new ResultadoImportacionDto(0, 0, 0, List.of("El archivo no contiene filas de datos."));
        }
        Map<String, Integer> columnas = columnas(filas.get(0), mapeo);
        List<String> errores = validarColumnas(columnas);
        if (!errores.isEmpty()) {
            return new ResultadoImportacionDto(0, 0, 0, errores);
        }

        Map<String, List<Fila>> grupos = new LinkedHashMap<>();
        for (int i = 1; i < filas.size(); i++) {
            if (i > MAX_FILAS) {
                errores.add("Se importaron solo las primeras " + MAX_FILAS + " filas.");
                break;
            }
            List<String> fila = filas.get(i);
            int numeroLinea = i + 1;
            String grupo = valor(fila, columnas, "grupo");
            if (grupo.isBlank()) {
                errores.add("Línea " + numeroLinea + ": el grupo está vacío.");
                continue;
            }
            grupos.computeIfAbsent(grupo, k -> new ArrayList<>()).add(new Fila(numeroLinea, fila));
        }

        int emitidas = 0;
        int rechazadas = 0;
        int lineas = 0;
        for (Map.Entry<String, List<Fila>> entrada : grupos.entrySet()) {
            try {
                FacturaRequest request = construir(entrada.getKey(), entrada.getValue(), columnas);
                ResultadoEmisionDto resultado = facturacion.emitir(empresaId, usuarioId, request);
                if (resultado.exitoso()) {
                    emitidas++;
                    lineas += request.lineas().size();
                } else {
                    rechazadas++;
                    String detalle = resultado.errores() == null || resultado.errores().isEmpty()
                            ? resultado.mensaje()
                            : resultado.errores().get(0).mensaje();
                    errores.add("Grupo " + entrada.getKey() + ": " + detalle);
                }
            } catch (IllegalArgumentException ex) {
                rechazadas++;
                errores.add("Grupo " + entrada.getKey() + ": " + ex.getMessage());
            }
        }
        return new ResultadoImportacionDto(emitidas, rechazadas, lineas, errores);
    }

    public List<FacturaRequest> preparar(byte[] contenido, String nombreArchivo, Map<String, Integer> mapeo) {
        List<List<String>> filas = LectorFilasImportacion.leer(contenido, nombreArchivo);
        if (filas.size() < 2) {
            throw new IllegalArgumentException("El archivo no contiene filas de datos.");
        }
        Map<String, Integer> columnas = columnas(filas.get(0), mapeo);
        List<String> errores = validarColumnas(columnas);
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errores));
        }
        Map<String, List<Fila>> grupos = new LinkedHashMap<>();
        for (int i = 1; i < filas.size(); i++) {
            if (i > MAX_FILAS) {
                break;
            }
            List<String> fila = filas.get(i);
            int numeroLinea = i + 1;
            String grupo = valor(fila, columnas, "grupo");
            if (grupo.isBlank()) {
                continue;
            }
            grupos.computeIfAbsent(grupo, k -> new ArrayList<>()).add(new Fila(numeroLinea, fila));
        }
        List<FacturaRequest> preparadas = new ArrayList<>();
        for (Map.Entry<String, List<Fila>> entrada : grupos.entrySet()) {
            preparadas.add(construir(entrada.getKey(), entrada.getValue(), columnas));
        }
        return preparadas;
    }

    private static List<String> validarColumnas(Map<String, Integer> columnas) {
        List<String> errores = new ArrayList<>();
        for (String requerida : List.of("grupo", "adquirente_numero_documento", "adquirente_nombre",
                "descripcion", "cantidad", "valor_unitario")) {
            if (!columnas.containsKey(requerida)) {
                errores.add("Falta la columna obligatoria: " + requerida);
            }
        }
        return errores;
    }

    private FacturaRequest construir(String grupo, List<Fila> filas, Map<String, Integer> columnas) {
        List<String> base = filas.get(0).valores();
        List<LineaFacturaRequest> lineas = new ArrayList<>();
        for (Fila fila : filas) {
            lineas.add(new LineaFacturaRequest(
                    defecto(valor(fila.valores(), columnas, "codigo_producto"), "IMPORT"),
                    requerido(valor(fila.valores(), columnas, "descripcion"), fila.numero(), "descripción"),
                    decimal(valor(fila.valores(), columnas, "cantidad"), fila.numero(), "cantidad"),
                    defecto(valor(fila.valores(), columnas, "unidad_medida"), "94"),
                    decimal(valor(fila.valores(), columnas, "valor_unitario"), fila.numero(), "valor unitario"),
                    decimalOpcional(valor(fila.valores(), columnas, "descuento"), fila.numero(), "descuento"),
                    decimalOpcional(valor(fila.valores(), columnas, "iva_pct"), fila.numero(), "IVA"),
                    decimalOpcional(valor(fila.valores(), columnas, "inc_pct"), fila.numero(), "INC"),
                    vacioANull(valor(fila.valores(), columnas, "unspsc"))));
        }

        AdquirenteRequest adquirente = new AdquirenteRequest(
                defecto(valor(base, columnas, "adquirente_tipo_documento"), "31"),
                requerido(valor(base, columnas, "adquirente_numero_documento"), filas.get(0).numero(), "documento del adquirente"),
                enteroOpcional(valor(base, columnas, "adquirente_digito_verificacion"), filas.get(0).numero(), "dígito de verificación"),
                requerido(valor(base, columnas, "adquirente_nombre"), filas.get(0).numero(), "nombre del adquirente"),
                vacioANull(valor(base, columnas, "adquirente_email")),
                vacioANull(valor(base, columnas, "adquirente_direccion")),
                vacioANull(valor(base, columnas, "adquirente_municipio")),
                defecto(valor(base, columnas, "adquirente_regimen"), "ORDINARIO"),
                defecto(valor(base, columnas, "adquirente_responsabilidad_fiscal"), "R-99-PN"));

        return new FacturaRequest(
                defecto(valor(base, columnas, "tipo_documento"), "01"),
                adquirente,
                lineas,
                vacioANull(valor(base, columnas, "observaciones")),
                defecto(valor(base, columnas, "forma_pago"), "1"),
                defecto(valor(base, columnas, "medio_pago"), "10"),
                vacioANull(valor(base, columnas, "tipo_operacion")),
                fechaOpcional(valor(base, columnas, "fecha_vencimiento"), filas.get(0).numero()),
                null,
                grupo,
                null);
    }

    private static Map<String, Integer> columnas(List<String> encabezado) {
        Map<String, Integer> columnas = new LinkedHashMap<>();
        for (int i = 0; i < encabezado.size(); i++) {
            String nombre = normalizar(encabezado.get(i));
            if (!nombre.isBlank()) {
                columnas.putIfAbsent(nombre, i);
            }
        }
        return columnas;
    }

    private static Map<String, Integer> columnas(List<String> encabezado, Map<String, Integer> mapeo) {
        if (mapeo == null || mapeo.isEmpty()) {
            return columnas(encabezado);
        }
        Map<String, Integer> columnas = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entrada : mapeo.entrySet()) {
            if (entrada.getKey() == null || entrada.getKey().isBlank()
                    || entrada.getValue() == null || entrada.getValue() < 0) {
                continue;
            }
            columnas.put(entrada.getKey(), entrada.getValue());
        }
        return columnas;
    }

    private static String normalizar(String texto) {
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
    }

    private static String valor(List<String> fila, Map<String, Integer> columnas, String columna) {
        Integer indice = columnas.get(columna);
        if (indice == null || indice >= fila.size()) {
            return "";
        }
        return fila.get(indice).trim();
    }

    private static String requerido(String valor, int linea, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Línea " + linea + ": falta " + campo + ".");
        }
        return valor;
    }

    private static String defecto(String valor, String porDefecto) {
        return valor == null || valor.isBlank() ? porDefecto : valor;
    }

    private static String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor;
    }

    private static BigDecimal decimal(String valor, int linea, String campo) {
        try {
            return new BigDecimal(valor.replace(",", "."));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Línea " + linea + ": " + campo + " no es numérico.");
        }
    }

    private static BigDecimal decimalOpcional(String valor, int linea, String campo) {
        return valor == null || valor.isBlank() ? BigDecimal.ZERO : decimal(valor, linea, campo);
    }

    private static Integer enteroOpcional(String valor, int linea, String campo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Línea " + linea + ": " + campo + " no es numérico.");
        }
    }

    private static LocalDate fechaOpcional(String valor, int linea) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Línea " + linea + ": fecha de vencimiento inválida (use AAAA-MM-DD).");
        }
    }

    private record Fila(int numero, List<String> valores) {
    }
}
