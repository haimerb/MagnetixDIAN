package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "cuenta_contable")
public class CuentaContableJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id")
    private Long empresaId;

    @Column(nullable = false, length = 10)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "cuenta_padre_id")
    private Long cuentaPadreId;

    @Column(nullable = false, length = 10)
    private String naturaleza = "DEUDORA";

    @Column(nullable = false)
    private int nivel = 1;

    @Column(name = "version_puc", nullable = false, length = 10)
    private String versionPuc = "2025";

    @Column(name = "acepta_auxiliar", nullable = false)
    private boolean aceptaAuxiliar = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public Long getCuentaPadreId() {
        return cuentaPadreId;
    }

    public void setCuentaPadreId(Long cuentaPadreId) {
        this.cuentaPadreId = cuentaPadreId;
    }

    public String getNaturaleza() {
        return naturaleza;
    }

    public void setNaturaleza(String naturaleza) {
        this.naturaleza = naturaleza;
    }

    public int getNivel() {
        return nivel;
    }

    public void setNivel(int nivel) {
        this.nivel = nivel;
    }

    public String getVersionPuc() {
        return versionPuc;
    }

    public void setVersionPuc(String versionPuc) {
        this.versionPuc = versionPuc;
    }

    public boolean isAceptaAuxiliar() {
        return aceptaAuxiliar;
    }

    public void setAceptaAuxiliar(boolean aceptaAuxiliar) {
        this.aceptaAuxiliar = aceptaAuxiliar;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
