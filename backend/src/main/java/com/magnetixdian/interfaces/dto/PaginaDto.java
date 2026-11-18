package com.magnetixdian.interfaces.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envoltura genérica de resultados paginados para la API REST.
 * Aísla a los clientes de la estructura interna de Spring Data.
 */
public record PaginaDto<T>(
        List<T> contenido,
        int pagina,
        int tamano,
        long total,
        int totalPaginas,
        boolean primera,
        boolean ultima
) {

    public static <T> PaginaDto<T> desde(Page<T> page) {
        return new PaginaDto<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast());
    }
}
