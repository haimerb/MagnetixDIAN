package com.magnetixdian.infrastructure.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convierte transparentemente a/desde la base de datos los campos sensibles de
 * la configuración DIAN usando {@link CifradorAes}. Si el cifrador no está
 * disponible (p. ej. pruebas sin Spring) el valor se conserva tal cual.
 */
@Converter
public class CifradoAtributoConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String atributo) {
        CifradorAes cifrador = CifradorAes.instancia();
        return (cifrador == null || atributo == null) ? atributo : cifrador.cifrar(atributo);
    }

    @Override
    public String convertToEntityAttribute(String columna) {
        CifradorAes cifrador = CifradorAes.instancia();
        return (cifrador == null || columna == null) ? columna : cifrador.descifrar(columna);
    }
}
