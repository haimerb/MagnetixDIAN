package com.magnetixdian.infrastructure.crypto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void escribeSobresV2YDescifraSobresV1() {
        CifradorAes cifrador = new CifradorAes("clave-de-prueba");
        String v2 = cifrador.cifrar("secreto-versionado");
        assertThat(v2).startsWith("enc:v2:primary:");
        assertThat(cifrador.descifrar(v2)).isEqualTo("secreto-versionado");

        String[] partes = v2.split(":", 4);
        String legado = CifradorAes.PREFIJO + partes[3];
        assertThat(cifrador.estaCifrado(legado)).isTrue();
        assertThat(cifrador.descifrar(legado)).isEqualTo("secreto-versionado");
    }

    @Test
    void rotaClavesConIdentificadoresExtra() {
        CifradorAes anterior = new CifradorAes("clave-antigua", "", false, "antigua");
        String cifradoAnterior = anterior.cifrar("credencial-rotada");
        assertThat(cifradoAnterior).startsWith("enc:v2:antigua:");

        CifradorAes actual = new CifradorAes("clave-nueva", "antigua:clave-antigua", true);
        assertThat(actual.descifrar(cifradoAnterior)).isEqualTo("credencial-rotada");
        assertThat(actual.cifrar("otra-credencial")).startsWith("enc:v2:primary:");
    }

    @Test
    void identificadorDeClaveDesconocidoFallaSiempre() {
        String cifradoConOtraClave = new CifradorAes("clave-activa", "", false, "retirada")
                .cifrar("secreto-rotado");

        assertThatThrownBy(() -> new CifradorAes("clave-activa").descifrar(cifradoConOtraClave))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Identificador de clave desconocido");
        assertThatThrownBy(() -> new CifradorAes("clave-activa", "", true).descifrar(cifradoConOtraClave))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Identificador de clave desconocido");
    }

    @Test
    void failFastRechazaLaClavePorDefecto() {
        assertThatThrownBy(() -> new CifradorAes(CifradorAes.CLAVE_DEV, "", true))
                .isInstanceOf(IllegalStateException.class);
        assertThat(new CifradorAes(CifradorAes.CLAVE_DEV).cifrar("valor")).startsWith("enc:v2:");
    }
}
