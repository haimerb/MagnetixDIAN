package com.magnetixdian.infrastructure.dian;

import com.magnetixdian.domain.dian.ArchivoListadoDian;
import com.magnetixdian.domain.dian.OrigenListadoDian;
import com.magnetixdian.domain.dian.ProveedorListadosDian;
import com.magnetixdian.domain.dian.SolicitudListadoDian;
import org.springframework.stereotype.Component;

@Component
public class ArchivoDianAdapter implements ProveedorListadosDian {

    @Override
    public OrigenListadoDian origen() {
        return OrigenListadoDian.ARCHIVO;
    }

    @Override
    public boolean soporta(SolicitudListadoDian solicitud) {
        return solicitud != null && solicitud.archivo() != null && solicitud.archivo().length > 0;
    }

    @Override
    public ArchivoListadoDian obtener(SolicitudListadoDian solicitud) {
        if (!soporta(solicitud)) {
            throw new IllegalArgumentException("Adjunte el archivo exportado por la DIAN.");
        }
        return new ArchivoListadoDian(solicitud.nombreArchivo(), solicitud.archivo());
    }
}
