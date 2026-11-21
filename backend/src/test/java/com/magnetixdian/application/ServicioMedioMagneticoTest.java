package com.magnetixdian.application;

import com.magnetixdian.domain.exogena.FormatoExogenaRegistry;
import com.magnetixdian.infrastructure.excel.ImportadorExcel;
import com.magnetixdian.infrastructure.excel.PlantillaExcelGenerator;
import com.magnetixdian.infrastructure.persistence.DetalleErrorRepository;
import com.magnetixdian.infrastructure.persistence.EmpresaRepository;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoJpa;
import com.magnetixdian.infrastructure.persistence.MedioMagneticoRepository;
import com.magnetixdian.infrastructure.persistence.OperacionRepository;
import com.magnetixdian.infrastructure.persistence.ValidacionRepository;
import com.magnetixdian.infrastructure.xml.GeneradorXml1001;
import com.magnetixdian.infrastructure.xml.GeneradorXml1002;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioMedioMagneticoTest {

    @Mock
    private MedioMagneticoRepository medioRepo;
    @Mock
    private OperacionRepository operacionRepo;
    @Mock
    private EmpresaRepository empresaRepo;
    @Mock
    private ValidacionRepository validacionRepo;
    @Mock
    private DetalleErrorRepository detalleRepo;
    @Mock
    private ImportadorExcel importadorExcel;
    @Mock
    private PlantillaExcelGenerator plantillaExcel;
    @Mock
    private ServicioValidacion servicioValidacion;
    @Mock
    private GeneradorXml1001 generadorXml1001;
    @Mock
    private GeneradorXml1002 generadorXml1002;

    private ServicioMedioMagnetico servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioMedioMagnetico(
                medioRepo, operacionRepo, empresaRepo, validacionRepo, detalleRepo,
                importadorExcel, plantillaExcel, servicioValidacion,
                generadorXml1001, generadorXml1002, new FormatoExogenaRegistry());
    }

    @Test
    void formatoInactivoNoGeneraXml() {
        MedioMagneticoJpa medio = new MedioMagneticoJpa(null, "1003", 2025);
        medio.setEstado("VALIDADO");
        when(medioRepo.findById(1L)).thenReturn(Optional.of(medio));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> servicio.generarXml(1L));

        assertTrue(error.getMessage().contains("1003"));
        verifyNoInteractions(operacionRepo, generadorXml1001, generadorXml1002);
    }

    @Test
    void formato1001SigueUsandoSuGenerador() {
        MedioMagneticoJpa medio = new MedioMagneticoJpa(null, "1001", 2025);
        medio.setEstado("VALIDADO");
        when(medioRepo.findById(1L)).thenReturn(Optional.of(medio));
        when(operacionRepo.findByMedioMagneticoId(1L)).thenReturn(List.of());
        when(generadorXml1001.generar(eq(medio), anyList())).thenReturn("<xml/>");

        assertEquals("<xml/>", servicio.generarXml(1L));
        verify(generadorXml1001).generar(eq(medio), anyList());
        verifyNoInteractions(generadorXml1002);
    }
}
