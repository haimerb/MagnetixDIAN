package com.magnetixdian.domain.firma;

import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.FileInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import org.xml.sax.InputSource;

/**
 * Firma XAdES-EPES (enveloped) con certificado PKCS#12, conforme al Anexo Técnico
 * FEV v1.9 (algoritmo SHA-256, política de firma DIAN v2).
 */
public class FirmadorXades implements FirmadorXml {

    private static final String NS_DS = "http://www.w3.org/2000/09/xmldsig#";
    private static final String NS_XADES = "http://uri.etsi.org/01903/v1.3.2#";
    private static final String POLICY_URL =
            "https://facturaelectronica.dian.gov.co/politicadefirma/v2/politicadefirmav2.pdf";
    private static final String POLICY_DESC =
            "Política de firma para facturas electrónicas de la República de Colombia.";
    private static final String POLICY_HASH = "dMoMvtcG5aIzgYo0tIsSQeVJBD7IZQ2ImM/7g2GRNRw=";

    @Override
    public String firmar(String xml, EmpresaFeConfigJpa config) {
        if (config == null || config.getCertificadoPath() == null || config.getCertificadoPath().isBlank()) {
            return xml;
        }
        try {
            Document doc = parse(xml);
            KeyStore ks = KeyStore.getInstance("PKCS12");
            char[] pwd = config.getCertificadoPassword() == null
                    ? new char[0] : config.getCertificadoPassword().toCharArray();
            try (FileInputStream fis = new FileInputStream(config.getCertificadoPath())) {
                ks.load(fis, pwd);
            }
            String alias = ks.aliases().nextElement();
            PrivateKey pk = (PrivateKey) ks.getKey(alias, pwd);
            X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

            Reference refDoc = fac.newReference("",
                    fac.newDigestMethod(DigestMethod.SHA256, null),
                    Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)),
                    null, null);

            String spId = "xmldsig-" + UUID.randomUUID();
            Element signedProperties = buildSignedProperties(doc, cert, spId);
            Reference refProps = fac.newReference("#" + spId,
                    fac.newDigestMethod(DigestMethod.SHA256, null),
                    Collections.singletonList(fac.newTransform(CanonicalizationMethod.EXCLUSIVE,
                            (TransformParameterSpec) null)),
                    "http://uri.etsi.org/01903#SignedProperties", null);

            SignedInfo si = fac.newSignedInfo(
                    fac.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null),
                    fac.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                    List.of(refDoc, refProps));

            KeyInfoFactory kif = fac.getKeyInfoFactory();
            X509Data x509Data = kif.newX509Data(Collections.singletonList(cert));
            KeyInfo ki = kif.newKeyInfo(Collections.singletonList(x509Data));

            Element qualifyingProperties = buildQualifyingProperties(doc, signedProperties);
            XMLObject xmlObject = fac.newXMLObject(
                    Collections.singletonList(new DOMStructure(qualifyingProperties)), null, null, null);

            DOMSignContext dsc = new DOMSignContext(pk, doc.getDocumentElement());
            dsc.setDefaultNamespacePrefix("ds");
            dsc.setIdAttributeNS(signedProperties, null, "Id");

            XMLSignature signature = fac.newXMLSignature(si, ki, Collections.singletonList(xmlObject), null, null);
            signature.sign(dsc);

            return serialize(doc);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el documento (XAdES): " + e.getMessage(), e);
        }
    }

    private Element buildSignedProperties(Document doc, X509Certificate cert, String spId) throws Exception {
        Element sp = doc.createElementNS(NS_XADES, "xades:SignedProperties");
        sp.setAttribute("Id", spId);

        Element sigProps = doc.createElementNS(NS_XADES, "xades:SignedSignatureProperties");
        Element signingTime = doc.createElementNS(NS_XADES, "xades:SigningTime");
        signingTime.setTextContent(OffsetDateTime.now().toString());
        sigProps.appendChild(signingTime);

        Element sigCert = doc.createElementNS(NS_XADES, "xades:SigningCertificateV2");
        Element certEl = doc.createElementNS(NS_XADES, "xades:Cert");
        Element certDigest = doc.createElementNS(NS_XADES, "xades:CertDigest");
        Element dm = doc.createElementNS(NS_DS, "ds:DigestMethod");
        dm.setAttribute("Algorithm", "http://www.w3.org/2001/04/xmlenc#sha256");
        Element dv = doc.createElementNS(NS_DS, "ds:DigestValue");
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        dv.setTextContent(Base64.getEncoder().encodeToString(md.digest(cert.getEncoded())));
        certDigest.appendChild(dm);
        certDigest.appendChild(dv);
        certEl.appendChild(certDigest);
        sigCert.appendChild(certEl);
        sigProps.appendChild(sigCert);

        Element policy = doc.createElementNS(NS_XADES, "xades:SignaturePolicyIdentifier");
        Element policyId = doc.createElementNS(NS_XADES, "xades:SignaturePolicyId");
        Element policyHash = doc.createElementNS(NS_XADES, "xades:SigPolicyHash");
        Element pdm = doc.createElementNS(NS_DS, "ds:DigestMethod");
        pdm.setAttribute("Algorithm", "http://www.w3.org/2001/04/xmlenc#sha256");
        Element pdv = doc.createElementNS(NS_DS, "ds:DigestValue");
        pdv.setTextContent(POLICY_HASH);
        policyHash.appendChild(pdm);
        policyHash.appendChild(pdv);
        policyId.appendChild(policyHash);
        Element policyIdEl = doc.createElementNS(NS_XADES, "xades:SigPolicyId");
        Element policyIdRef = doc.createElementNS(NS_XADES, "xades:Identifier");
        policyIdRef.setAttribute("Qualifier", "OIDAsURI");
        policyIdRef.setTextContent(POLICY_URL);
        Element policyDesc = doc.createElementNS(NS_XADES, "xades:Description");
        policyDesc.setTextContent(POLICY_DESC);
        policyIdEl.appendChild(policyIdRef);
        policyIdEl.appendChild(policyDesc);
        policyId.appendChild(policyIdEl);
        policy.appendChild(policyId);
        sigProps.appendChild(policy);

        sp.appendChild(sigProps);
        return sp;
    }

    private Element buildQualifyingProperties(Document doc, Element signedProperties) {
        Element qp = doc.createElementNS(NS_XADES, "xades:QualifyingProperties");
        qp.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xades", NS_XADES);
        qp.setAttribute("Target", "");
        qp.appendChild(signedProperties);
        return qp;
    }

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private String serialize(Document doc) throws Exception {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter w = new StringWriter();
        t.transform(new DOMSource(doc), new StreamResult(w));
        return w.toString();
    }
}
