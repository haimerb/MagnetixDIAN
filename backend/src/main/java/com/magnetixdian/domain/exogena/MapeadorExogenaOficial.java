package com.magnetixdian.domain.exogena;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class MapeadorExogenaOficial {

    public static final String FORMATO_1125 = "1125";
    public static final String FORMATO_2278 = "2278";
    public static final String FORMATO_5253 = "5253";

    private static final Map<String, List<String>> CAMPOS = new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> ALIAS = new LinkedHashMap<>();
    private static final Map<String, List<String>> OBLIGATORIOS = new LinkedHashMap<>();
    private static final Map<String, Map<String, String>> VALORES_POR_DEFECTO = new LinkedHashMap<>();
    private static final Map<String, List<ConceptoExogena>> CONCEPTOS = new LinkedHashMap<>();
    private static final Map<String, String> ELEMENTOS = Map.of(
            FORMATO_1125, "item",
            FORMATO_2278, "comprabonos",
            FORMATO_5253, "bene");
    private static final Map<String, String> ATRIBUTO_TOTAL = Map.of(
            FORMATO_1125, "monto",
            FORMATO_2278, "vtotal",
            FORMATO_5253, "vtrupj");
    private static final Map<String, Integer> LIMITE_REGISTROS = Map.of(
            FORMATO_1125, 9999,
            FORMATO_2278, 9999,
            FORMATO_5253, 5000);
    private static final Set<String> ATRIBUTOS_ENTEROS = Set.of(
            "tdoc", "tdocb", "tipvinc", "top", "pais", "paina", "paire", "natdeope",
            "globseg", "tiprang", "paranlz", "clasebn", "tinfo", "papjpd", "prupjpd");
    private static final Set<String> ATRIBUTOS_DINERO = Set.of(
            "monto", "montoprin", "saldoprin", "ajuste", "vtotal",
            "vtrupjbe", "vtprpj", "vtrupj", "papj", "prupj");
    private static final Set<String> ATRIBUTOS_IDENTIFICACION = Set.of("nid", "nitb");
    private static final Set<String> ATRIBUTOS_SI_NO = Set.of("reest", "accost", "cosdedren");
    private static final Set<String> ATRIBUTOS_FECHA = Set.of("fena", "fein", "fefi");

    static {
        CAMPOS.put(FORMATO_1125, List.of(
                "tdoc", "nid", "apl1", "apl2", "nom1", "nom2", "raz", "tipvinc", "top", "pais",
                "ciudad", "natdeope", "monto", "cosdedren", "montoprin", "saldoprin", "met",
                "indrent", "spos", "sneg", "apos", "aneg", "vaco", "globseg", "tipajus", "tiprang",
                "liminp", "med", "limsup", "liminn", "medn", "limsun", "paranlz", "ajuste",
                "reest", "accost"));
        CAMPOS.put(FORMATO_2278, List.of(
                "tdocb", "nitb", "pap", "sap", "pno", "ono", "raz", "dir", "dpto", "mun",
                "pais", "email", "clasebn", "vtotal"));
        CAMPOS.put(FORMATO_5253, List.of(
                "tinfo", "tdoc", "nid", "apl1", "apl2", "nom1", "nom2", "fena", "paina", "dir",
                "dpto", "mun", "paire", "email", "fein", "fefi", "papj", "papjpd", "prupj",
                "prupjpd", "vtrupjbe", "vtprpj", "vtrupj", "ifide"));

        OBLIGATORIOS.put(FORMATO_1125, List.of(
                "tdoc", "nid", "tipvinc", "top", "pais", "ciudad", "monto", "reest", "accost"));
        OBLIGATORIOS.put(FORMATO_2278, List.of("tdocb", "nitb", "pais", "clasebn", "vtotal"));
        OBLIGATORIOS.put(FORMATO_5253, List.of(
                "tinfo", "tdoc", "nid", "apl1", "nom1", "paina", "paire",
                "papj", "papjpd", "prupj", "prupjpd", "vtrupjbe", "vtprpj", "vtrupj"));

        VALORES_POR_DEFECTO.put(FORMATO_1125, Map.of("pais", "1", "reest", "NO", "accost", "NO"));
        VALORES_POR_DEFECTO.put(FORMATO_2278, Map.of("pais", "1"));
        VALORES_POR_DEFECTO.put(FORMATO_5253, Map.of(
                "paina", "1", "paire", "1", "papj", "0", "papjpd", "0",
                "prupj", "0", "prupjpd", "0"));

        CONCEPTOS.put(FORMATO_1125, List.of(
                new ConceptoExogena(FORMATO_1125, "PT", "Declaracion informativa individual de precios de transferencia", 1),
                new ConceptoExogena(FORMATO_1125, "PT-ANEXO", "Informacion anexa de precios de transferencia", 2)));
        CONCEPTOS.put(FORMATO_2278, List.of(
                new ConceptoExogena(FORMATO_2278, "BE", "Compra de bonos electronicos", 1),
                new ConceptoExogena(FORMATO_2278, "BE-DET", "Detalle de bonos electronicos", 2)));
        CONCEPTOS.put(FORMATO_5253, List.of(
                new ConceptoExogena(FORMATO_5253, "BF", "Beneficiarios efectivos", 1),
                new ConceptoExogena(FORMATO_5253, "BF-DET", "Detalle de beneficiarios efectivos", 2)));

        for (Map.Entry<String, List<String>> entrada : CAMPOS.entrySet()) {
            Map<String, String> alias = new LinkedHashMap<>();
            for (String campo : entrada.getValue()) {
                alias.put(normalizar(campo), campo);
            }
            ALIAS.put(entrada.getKey(), alias);
        }

        alias(FORMATO_1125, "tdoc", "TIPO_DOCUMENTO", "TIPODOCUMENTO", "TDOCUMENTO");
        alias(FORMATO_1125, "nid", "NIT", "IDENTIFICACION", "NUMERO_IDENTIFICACION", "NUMEROIDENTIFICACION");
        alias(FORMATO_1125, "apl1", "APELLIDO1", "PRIMER_APELLIDO");
        alias(FORMATO_1125, "apl2", "APELLIDO2", "SEGUNDO_APELLIDO");
        alias(FORMATO_1125, "nom1", "NOMBRE1", "PRIMER_NOMBRE");
        alias(FORMATO_1125, "nom2", "NOMBRE2", "SEGUNDO_NOMBRE");
        alias(FORMATO_1125, "raz", "RAZON_SOCIAL");
        alias(FORMATO_1125, "monto", "VALOR", "VALOR_PAGO", "BASE_IMPONIBLE");
        alias(FORMATO_1125, "ciudad", "CIUDAD", "MUNICIPIO");

        alias(FORMATO_2278, "tdocb", "TDOC", "TIPO_DOCUMENTO", "TIPODOCUMENTO", "TIPO_DOCUMENTO_BENEFICIARIO");
        alias(FORMATO_2278, "nitb", "NIT", "NID", "IDENTIFICACION", "NUMERO_IDENTIFICACION");
        alias(FORMATO_2278, "pap", "APELLIDO1", "PRIMER_APELLIDO");
        alias(FORMATO_2278, "sap", "APELLIDO2", "SEGUNDO_APELLIDO");
        alias(FORMATO_2278, "pno", "NOMBRE1", "PRIMER_NOMBRE");
        alias(FORMATO_2278, "ono", "NOMBRE2", "SEGUNDO_NOMBRE");
        alias(FORMATO_2278, "raz", "RAZON_SOCIAL");
        alias(FORMATO_2278, "clasebn", "CLASE_BONO");
        alias(FORMATO_2278, "vtotal", "VALOR", "VALOR_TOTAL", "VALOR_PAGO");
        alias(FORMATO_2278, "email", "CORREO", "CORREO_ELECTRONICO");

        alias(FORMATO_5253, "nid", "NIT", "IDENTIFICACION", "NUMERO_IDENTIFICACION");
        alias(FORMATO_5253, "apl1", "APELLIDO1", "PRIMER_APELLIDO");
        alias(FORMATO_5253, "apl2", "APELLIDO2", "SEGUNDO_APELLIDO");
        alias(FORMATO_5253, "nom1", "NOMBRE1", "PRIMER_NOMBRE");
        alias(FORMATO_5253, "nom2", "NOMBRE2", "SEGUNDO_NOMBRE");
        alias(FORMATO_5253, "fena", "FECHA_NACIMIENTO");
        alias(FORMATO_5253, "paina", "NACIONALIDAD");
        alias(FORMATO_5253, "paire", "PAIS_RESIDENCIA", "RESIDENCIA");
        alias(FORMATO_5253, "email", "CORREO", "CORREO_ELECTRONICO");
        alias(FORMATO_5253, "vtrupjbe", "VALOR_BENEFICIARIO");
        alias(FORMATO_5253, "vtprpj", "VALOR_PARTICIPACION");
        alias(FORMATO_5253, "vtrupj", "VALOR_TOTAL", "VALOR");
    }

    public static List<String> encabezados(String formato) {
        String codigo = validarFormato(formato);
        return CAMPOS.get(codigo).stream()
                .map(campo -> campo.toUpperCase(Locale.ROOT))
                .toList();
    }

    public List<ConceptoExogena> conceptos(String formato) {
        String codigo = validarFormato(formato);
        return List.copyOf(CONCEPTOS.get(codigo));
    }

    public DocumentoExogena mapear(String formato, JsonNode datos, CabeceraExogena cabecera) {
        String codigo = validarFormato(formato);
        if (cabecera == null || datos == null || !datos.isObject()) {
            throw new IllegalArgumentException("Datos incompletos para generar el formato " + codigo);
        }
        JsonNode anio = datos.get("anioGravable");
        if (anio != null && anio.isNumber() && anio.asInt() != cabecera.anio()) {
            throw new IllegalArgumentException("El año de los datos no coincide con la cabecera del formato " + codigo);
        }
        JsonNode filas = datos.get("filas");
        if (filas == null || !filas.isArray() || filas.isEmpty()) {
            throw new IllegalArgumentException("El formato " + codigo + " requiere al menos una fila");
        }
        if (filas.size() > LIMITE_REGISTROS.get(codigo)) {
            throw new IllegalArgumentException("El formato " + codigo + " admite máximo "
                    + LIMITE_REGISTROS.get(codigo) + " registros");
        }

        int codCpt = codigoConcepto(codigo, datos, cabecera);
        List<RegistroExogena> registros = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < filas.size(); i++) {
            JsonNode fila = filas.get(i);
            Map<String, String> atributos = mapearFila(codigo, fila, i + 1, cabecera);
            validarObligatorios(codigo, atributos, i + 1);
            String valor = atributos.get(ATRIBUTO_TOTAL.get(codigo));
            if (valor != null && !valor.isBlank()) {
                total = total.add(new BigDecimal(valor));
            }
            registros.add(new RegistroExogena(i + 1, Collections.unmodifiableMap(atributos)));
        }

        return new DocumentoExogena(
                codigo,
                cabecera.version(),
                cabecera.anio(),
                codCpt,
                cabecera.numEnvio(),
                cabecera.fecEnvio(),
                cabecera.fecInicial(),
                cabecera.fecFinal(),
                total.stripTrailingZeros().toPlainString(),
                registros.size(),
                ELEMENTOS.get(codigo),
                List.copyOf(registros));
    }

    private Map<String, String> mapearFila(String formato, JsonNode fila, int linea, CabeceraExogena cabecera) {
        if (fila == null || !fila.isObject()) {
            throw new IllegalArgumentException("Fila " + linea + " del formato " + formato + " no es un objeto");
        }
        Map<String, String> atributos = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> campos = fila.fields();
        while (campos.hasNext()) {
            Map.Entry<String, JsonNode> entry = campos.next();
            String campo = ALIAS.get(formato).get(normalizar(entry.getKey()));
            if (campo == null || entry.getValue().isNull()) {
                continue;
            }
            String valor = limpiar(campo, entry.getValue().asText(), linea);
            if (valor != null && !valor.isBlank()) {
                atributos.put(campo, valor);
            }
        }
        VALORES_POR_DEFECTO.get(formato).forEach(atributos::putIfAbsent);
        if (FORMATO_5253.equals(formato)) {
            atributos.putIfAbsent("fein", cabecera.fecInicial());
            atributos.putIfAbsent("fefi", cabecera.fecFinal());
        }
        return atributos;
    }

    private void validarObligatorios(String formato, Map<String, String> atributos, int linea) {
        for (String campo : OBLIGATORIOS.get(formato)) {
            String valor = atributos.get(campo);
            if (valor == null || valor.isBlank()) {
                throw new IllegalArgumentException("Fila " + linea + " del formato " + formato
                        + " sin campo obligatorio " + campo);
            }
        }
    }

    private int codigoConcepto(String formato, JsonNode datos, CabeceraExogena cabecera) {
        String concepto = cabecera.concepto();
        if ((concepto == null || concepto.isBlank()) && datos.path("filas").isArray()
                && !datos.path("filas").isEmpty()) {
            JsonNode primera = datos.path("filas").get(0);
            if (primera.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> campos = primera.fields();
                while (campos.hasNext()) {
                    Map.Entry<String, JsonNode> entry = campos.next();
                    String clave = normalizar(entry.getKey());
                    if ("CONCEPTO".equals(clave) || "CODIGOCONCEPTO".equals(clave)
                            || "CONCEPTOCONTABLE".equals(clave)) {
                        concepto = entry.getValue().asText();
                        break;
                    }
                }
            }
        }
        List<ConceptoExogena> disponibles = CONCEPTOS.get(formato);
        if (concepto == null || concepto.isBlank()) {
            return disponibles.get(0).codCpt();
        }
        String buscado = normalizar(concepto);
        for (ConceptoExogena item : disponibles) {
            if (buscado.equals(normalizar(item.codigo()))
                    || buscado.contains(normalizar(item.codigo()))
                    || normalizar(item.descripcion()).contains(buscado)) {
                return item.codCpt();
            }
        }
        throw new IllegalArgumentException("Concepto " + concepto + " no aplica al formato " + formato);
    }

    private String limpiar(String campo, String valor, int linea) {
        String resultado = valor == null ? "" : valor.trim();
        if (resultado.isBlank()) {
            return null;
        }
        if (ATRIBUTOS_IDENTIFICACION.contains(campo)) {
            return resultado.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        }
        if (ATRIBUTOS_SI_NO.contains(campo)) {
            String siNo = resultado.toUpperCase(Locale.ROOT);
            if (!"SI".equals(siNo) && !"NO".equals(siNo)) {
                throw new IllegalArgumentException("Fila " + linea + ": " + campo + " debe ser SI o NO");
            }
            return siNo;
        }
        if (ATRIBUTOS_ENTEROS.contains(campo) || ATRIBUTOS_DINERO.contains(campo)) {
            return soloDigitos(resultado, campo, linea);
        }
        if (ATRIBUTOS_FECHA.contains(campo)) {
            return fecha(resultado, campo, linea);
        }
        if ("dpto".equals(campo) || "mun".equals(campo) || "ifide".equals(campo)) {
            return resultado;
        }
        return resultado;
    }

    private String soloDigitos(String valor, String campo, int linea) {
        String digitos = valor.replaceAll("\\D", "");
        if (digitos.isEmpty()) {
            throw new IllegalArgumentException("Fila " + linea + ": " + campo + " debe ser numérico");
        }
        return digitos;
    }

    private String fecha(String valor, String campo, int linea) {
        try {
            return LocalDate.parse(valor, DateTimeFormatter.ISO_LOCAL_DATE).toString();
        } catch (DateTimeParseException ignorado) {
            try {
                return LocalDate.parse(valor, DateTimeFormatter.ofPattern("dd/MM/yyyy")).toString();
            } catch (DateTimeParseException ignoradoTambien) {
                try {
                    return LocalDate.parse(valor, DateTimeFormatter.ofPattern("yyyy/MM/dd")).toString();
                } catch (DateTimeParseException ex) {
                    throw new IllegalArgumentException("Fila " + linea + ": " + campo
                            + " debe tener formato YYYY-MM-DD", ex);
                }
            }
        }
    }

    private static void alias(String formato, String campo, String... nombres) {
        Map<String, String> alias = ALIAS.get(formato);
        for (String nombre : nombres) {
            alias.put(normalizar(nombre), campo);
        }
    }

    private static String validarFormato(String formato) {
        String codigo = formato == null ? "" : formato.trim();
        if (!CAMPOS.containsKey(codigo)) {
            throw new IllegalArgumentException("Formato exógena oficial no soportado: " + codigo);
        }
        return codigo;
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }
}
