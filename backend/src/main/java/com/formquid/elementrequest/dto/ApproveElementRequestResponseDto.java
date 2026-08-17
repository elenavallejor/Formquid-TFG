package com.formquid.elementrequest.dto;

import com.formquid.elementrequest.entity.ElementRequestType;

/**
 * Resultado de aprobar una solicitud de elemento.
 *
 * El id corresponde al nuevo elemento creado en el catálogo. La solicitud
 * original permanece almacenada como APPROVED.
 */
public record ApproveElementRequestResponseDto(
    ElementRequestType type,
    Long createdElementId
) {}
