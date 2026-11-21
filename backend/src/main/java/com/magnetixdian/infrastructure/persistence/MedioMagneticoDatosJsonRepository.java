package com.magnetixdian.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedioMagneticoDatosJsonRepository extends JpaRepository<MedioMagneticoJpa, Long> {

    @Modifying
    @Query(value = """
            UPDATE medio_magnetico
            SET datos_json = CAST(:datosJson AS jsonb), updated_at = now()
            WHERE id = :id
            """, nativeQuery = true)
    int actualizarDatosJson(@Param("id") Long id, @Param("datosJson") String datosJson);

    @Query(value = "SELECT datos_json::text FROM medio_magnetico WHERE id = :id", nativeQuery = true)
    String leerDatosJson(@Param("id") Long id);
}
