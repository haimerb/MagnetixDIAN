package com.magnetixdian.domain.dian;

public interface ProveedorListadosDian {

    OrigenListadoDian origen();

    boolean soporta(SolicitudListadoDian solicitud);

    ArchivoListadoDian obtener(SolicitudListadoDian solicitud);
}
