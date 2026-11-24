package com.magnetixdian.infrastructure.integracion.erp.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "erp_activo")
public class ErpActivoJpa extends ErpBaseJpa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 40)
    private String codigo;
    @Column(nullable = false, length = 200)
    private String nombre;
    @Column(length = 80)
    private String categoria;
    @Column(name = "fecha_alta", nullable = false)
    private LocalDate fechaAlta;
    @Column(name = "valor_adquisicion", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorAdquisicion;
    @Column(name = "valor_residual", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorResidual;
    @Column(name = "vida_util_meses", nullable = false)
    private Integer vidaUtilMeses;
    @Column(name = "depreciacion_acumulada", nullable = false, precision = 18, scale = 2)
    private BigDecimal depreciacionAcumulada;
    @Column(name = "valor_libros", nullable = false, precision = 18, scale = 2)
    private BigDecimal valorLibros;
    @Column(name = "fecha_baja")
    private LocalDate fechaBaja;
    @Column(length = 1000)
    private String observaciones;
    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public LocalDate getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDate fechaAlta) { this.fechaAlta = fechaAlta; }
    public BigDecimal getValorAdquisicion() { return valorAdquisicion; }
    public void setValorAdquisicion(BigDecimal valorAdquisicion) { this.valorAdquisicion = valorAdquisicion; }
    public BigDecimal getValorResidual() { return valorResidual; }
    public void setValorResidual(BigDecimal valorResidual) { this.valorResidual = valorResidual; }
    public Integer getVidaUtilMeses() { return vidaUtilMeses; }
    public void setVidaUtilMeses(Integer vidaUtilMeses) { this.vidaUtilMeses = vidaUtilMeses; }
    public BigDecimal getDepreciacionAcumulada() { return depreciacionAcumulada; }
    public void setDepreciacionAcumulada(BigDecimal depreciacionAcumulada) { this.depreciacionAcumulada = depreciacionAcumulada; }
    public BigDecimal getValorLibros() { return valorLibros; }
    public void setValorLibros(BigDecimal valorLibros) { this.valorLibros = valorLibros; }
    public LocalDate getFechaBaja() { return fechaBaja; }
    public void setFechaBaja(LocalDate fechaBaja) { this.fechaBaja = fechaBaja; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
}
