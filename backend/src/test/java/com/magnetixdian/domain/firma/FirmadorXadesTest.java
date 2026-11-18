package com.magnetixdian.domain.firma;

import com.magnetixdian.infrastructure.persistence.EmpresaFeConfigJpa;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class FirmadorXadesTest {

    private static final String XML =
            "<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\">"
                    + "<cbc:ID xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">FV-1</cbc:ID>"
                    + "</Invoice>";

    private KeyPair keyPair;
    private Path keystorePath;
    private final char[] password = "clave123".toCharArray();

    private void crearKeystore(Path dir) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        keyPair = kpg.generateKeyPair();
        X500Name dn = new X500Name("CN=MagnetixDIAN Test, O=MagnetixDIAN");
        Date desde = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
        Date hasta = Date.from(Instant.now().plus(365, ChronoUnit.DAYS));
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                dn, BigInteger.ONE, desde, hasta, dn, keyPair.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        X509CertificateHolder holder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(holder);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("test", keyPair.getPrivate(), password, new Certificate[]{cert});
        keystorePath = dir.resolve("certificado.p12");
        try (FileOutputStream out = new FileOutputStream(keystorePath.toFile())) {
            ks.store(out, password);
        }
    }

    private EmpresaFeConfigJpa config() {
        EmpresaFeConfigJpa c = new EmpresaFeConfigJpa();
        c.setCertificadoPath(keystorePath.toString());
        c.setCertificadoPassword(new String(password));
        return c;
    }

    @Test
    void firmaConCertificadoP12YProduceFirmaValida(@TempDir Path dir) throws Exception {
        crearKeystore(dir);
        String firmado = new FirmadorXades().firmar(XML, config());

        assertThat(firmado).contains("SignatureValue");
        assertThat(firmado).contains("xades:SignedProperties");

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document doc = factory.newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(firmado.getBytes(StandardCharsets.UTF_8)));
        NodeList firmas = doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");
        assertThat(firmas.getLength()).isEqualTo(1);

        Element firma = (Element) firmas.item(0);
        NodeList props = doc.getElementsByTagNameNS("http://uri.etsi.org/01903/v1.3.2#", "SignedProperties");
        DOMValidateContext ctx = new DOMValidateContext(keyPair.getPublic(), firma);
        ctx.setIdAttributeNS((Element) props.item(0), null, "Id");
        XMLSignature sig = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(ctx);
        assertThat(sig.getSignedInfo().getReferences()).hasSize(2);
        assertThat(sig.getSignatureValue().getValue()).isNotEmpty();
    }

    @Test
    void sinCertificadoDevuelveElXmlOriginal() {
        EmpresaFeConfigJpa vacia = new EmpresaFeConfigJpa();
        assertThat(new FirmadorXades().firmar(XML, vacia)).isEqualTo(XML);
    }
}
