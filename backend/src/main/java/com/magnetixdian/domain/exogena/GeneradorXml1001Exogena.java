package com.magnetixdian.domain.exogena;

import com.magnetixdian.interfaces.dto.DatosDeclaranteDto;
import com.magnetixdian.interfaces.dto.ExogenaRenglonDto;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.Year;
import java.util.List;
import java.util.Locale;

@Component
public class GeneradorXml1001Exogena {

    private static final String NS = "urn:magnetixdian:exogena:1001";

    public String generar(DatosDeclaranteDto declarante, List<ExogenaRenglonDto> renglones) {
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setNamespaceAware(true);
            Document documento = fabrica.newDocumentBuilder().newDocument();
            Integer ano = renglones != null && !renglones.isEmpty() && renglones.get(0).ano() != null
                    ? renglones.get(0).ano() : Year.now().getValue();
            Element raiz = documento.createElementNS(NS, "Exogena1001");
            raiz.setAttribute("ano", String.valueOf(ano));
            raiz.setAttribute("formato", FormatoTributarioCatalogo.FORMATO_1001);
            documento.appendChild(raiz);

            Element nodoDeclarante = documento.createElementNS(NS, "Declarante");
            nodoDeclarante.setAttribute("nit", valor(declarante == null ? null : declarante.nit()));
            nodoDeclarante.setAttribute("dv", valor(declarante == null ? null : declarante.digitoVerificacion()));
            nodoDeclarante.setAttribute("razonSocial", valor(declarante == null ? null : declarante.razonSocial()));
            nodoDeclarante.setAttribute("tipoDocumento", valor(declarante == null ? null : declarante.tipoDocumento()));
            nodoDeclarante.setAttribute("municipio", valor(declarante == null ? null : declarante.municipio()));
            nodoDeclarante.setAttribute("pais", valor(declarante == null ? null : declarante.pais()));
            raiz.appendChild(nodoDeclarante);

            Element nodoRenglones = documento.createElementNS(NS, "Renglones");
            raiz.appendChild(nodoRenglones);
            BigDecimal total = BigDecimal.ZERO;
            int consecutivo = 1;
            if (renglones != null) {
                for (ExogenaRenglonDto renglon : renglones) {
                    Element nodo = documento.createElementNS(NS, "Renglon");
                    nodo.setAttribute("consecutivo", String.valueOf(consecutivo++));
                    nodo.setAttribute("tipoDocumento", valor(renglon.tipoDocumento()));
                    nodo.setAttribute("numeroDocumento", valor(renglon.numeroDocumento()));
                    nodo.setAttribute("dv", valor(renglon.digitoVerificacion()));
                    nodo.setAttribute("razonSocial", valor(renglon.razonSocial()));
                    nodo.setAttribute("municipio", valor(renglon.municipio()));
                    nodo.setAttribute("pais", valor(renglon.pais()));
                    nodo.setAttribute("concepto", valor(renglon.codigoConcepto()));
                    nodo.setAttribute("descripcionConcepto", valor(renglon.descripcionConcepto()));
                    nodo.setAttribute("valor", decimal(renglon.valor()));
                    nodo.setAttribute("cantidadMovimientos", String.valueOf(renglon.cantidadMovimientos() == null ? 0 : renglon.cantidadMovimientos()));
                    nodoRenglones.appendChild(nodo);
                    total = total.add(renglon.valor() == null ? BigDecimal.ZERO : renglon.valor());
                }
            }

            Element nodoControl = documento.createElementNS(NS, "Control");
            nodoControl.setAttribute("totalRenglones", String.valueOf(renglones == null ? 0 : renglones.size()));
            nodoControl.setAttribute("totalValor", decimal(total));
            nodoControl.setAttribute("prevalidacionOficial", "false");
            raiz.appendChild(nodoControl);

            Transformer transformador = TransformerFactory.newInstance().newTransformer();
            transformador.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformador.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter escritor = new StringWriter();
            transformador.transform(new DOMSource(documento), new StreamResult(escritor));
            return escritor.toString();
        } catch (ParserConfigurationException | TransformerException ex) {
            throw new IllegalStateException("No fue posible generar el XML de exógena 1001", ex);
        }
    }

    private String valor(String texto) {
        return texto == null ? "" : texto;
    }

    private String decimal(BigDecimal valor) {
        return String.format(Locale.ROOT, "%.2f", valor == null ? BigDecimal.ZERO : valor);
    }
}
