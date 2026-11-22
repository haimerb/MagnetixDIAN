package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "conciliacion_linea")
public class ConciliacionLineaJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conciliacion_id", nullable = false)
    private ConciliacionJpa conciliacion;

    @Column(name = "documento_radian_id", nullable = false)
    private Long documentoRadianId;

    @Column(length = 50)
    private String consecutivo;

    @Column(name = "asiento_contable_id")
    private Long asientoContableId;

    @Column(nullable = false, length = 15)
    private String resultado;

    @Column(nullable = false, length = 15)
    private String severidad;

    @Column(nullable = false, length = 30)
    private String campo;

    @Column(name = "valor_documento", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorDocumento = BigDecimal.ZERO;

    @Column(name = "valor_asiento", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorAsiento = BigDecimal.ZERO;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal diferencia = BigDecimal.ZERO;

    @Column(length = 500)
    private String mensaje;

    public Long getId() {
        return id;
    }

    public ConciliacionJpa getConciliacion() {
        return conciliacion;
    }

    public void setConciliacion(ConciliacionJpa conciliacion) {
        this.conciliacion = conciliacion;
    }

    public Long getDocumentoRadianId() {
        return documentoRadianId;
    }

    public void setDocumentoRadianId(Long documentoRadianId) {
        this.documentoRadianId = documentoRadianId;
    }

    public String getConsecutivo() {
        return consecutivo;
    }

    public void setConsecutivo(String consecutivo) {
        this.consecutivo = consecutivo;
    }

    public Long getAsientoContableId() {
        return asientoContableId;
    }

    public void setAsientoContableId(Long asientoContableId) {
        this.asientoContableId = asientoContableId;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getSeveridad() {
        return severidad;
    }

    public void setSeveridad(String severidad) {
        this.severidad = severidad;
    }

    public String getCampo() {
        return campo;
    }

    public void setCampo(String campo) {
        this.campo = campo;
    }

    public BigDecimal getValorDocumento() {
        return valorDocumento;
    }

    public void setValorDocumento(BigDecimal valorDocumento) {
        this.valorDocumento = valorDocumento;
    }

    public BigDecimal getValorAsiento() {
        return valorAsiento;
    }

    public void setValorAsiento(BigDecimal valorAsiento) {
        this.valorAsiento = valorAsiento;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(BigDecimal diferencia) {
        this.diferencia = diferencia;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
