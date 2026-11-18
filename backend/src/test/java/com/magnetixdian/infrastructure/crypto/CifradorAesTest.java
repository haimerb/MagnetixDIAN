package com.magnetixdian.infrastructure.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CifradorAesTest {

    @Test
    void cifraYDescifraUnValor() {
        CifradorAes cifrador = new CifradorAes("clave-de-prueba");
        String cifrado = cifrador.cifrar("ClTec-12345");
        assertThat(cifrado).startsWith("enc:").isNotEqualTo("ClTec-12345");
        assertThat(cifrador.descifrar(cifrado)).isEqualTo("ClTec-12345");
    }

    @Test
    void noRecifraUnValorYaCifrado() {
        CifradorAes cifrador = new CifradorAes("clave-de-prueba");
        String cifrado = cifrador.cifrar("secreto");
        assertThat(cifrador.cifrar(cifrado)).isEqualTo(cifrado);
    }

    @Test
    void valorSinPrefijoSeDevuelveTalCual() {
        CifradorAes cifrador = new CifradorAes("clave-de-prueba");
        assertThat(cifrador.descifrar("texto-plano")).isEqualTo("texto-plano");
    }

    @Test
    void elConverterCifraYDescifraTransparentemente() {
        new CifradorAes("clave-de-prueba");
        CifradoAtributoConverter converter = new CifradoAtributoConverter();
        String enBd = converter.convertToDatabaseColumn("mi-api-key");
        assertThat(enBd).startsWith("enc:");
        assertThat(converter.convertToEntityAttribute(enBd)).isEqualTo("mi-api-key");
    }
}
