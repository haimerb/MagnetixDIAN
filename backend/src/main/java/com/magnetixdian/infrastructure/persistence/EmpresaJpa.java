package com.magnetixdian.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "empresa")
public class EmpresaJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String nit;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Column(length = 30, nullable = false)
    private String regimen = "ORDINARIO";

    @Column(name = "gran_contribuyente", nullable = false)
    private boolean granContribuyente;

    @Column(name = "tipo_documento", length = 10, nullable = false)
    private String tipoDocumento = "NIT";

    @Column(length = 150)
    private String direccion;

    @Column(length = 80)
    private String ciudad;

    @Column(length = 80)
    private String departamento;

    @Column(name = "codigo_dane", length = 10)
    private String codigoDane;

    @Column(length = 150)
    private String email;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected EmpresaJpa() {
    }

    public EmpresaJpa(String nit, String razonSocial) {
        this.nit = nit;
        this.razonSocial = razonSocial;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public String getRegimen() {
        return regimen;
    }

    public boolean isGranContribuyente() {
        return granContribuyente;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getCiudad() {
        return ciudad;
    }

    public String getDepartamento() {
        return departamento;
    }

    public String getCodigoDane() {
        return codigoDane;
    }

    public String getEmail() {
        return email;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public void setCodigoDane(String codigoDane) {
        this.codigoDane = codigoDane;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setGranContribuyente(boolean granContribuyente) {
        this.granContribuyente = granContribuyente;
    }

    public void setRegimen(String regimen) {
        this.regimen = regimen;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }
}