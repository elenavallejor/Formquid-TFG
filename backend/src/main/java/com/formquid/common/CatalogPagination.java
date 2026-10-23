package com.formquid.common;

import com.formquid.common.dto.CatalogSortOrder;
import com.formquid.common.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

public final class CatalogPagination {

    private CatalogPagination() {
    }

    public static Pageable catalogPageable(int page, int size, CatalogSortOrder sortOrder) {
        validatePage(page, size);
        Sort.Direction direction = sortOrder == CatalogSortOrder.OLDEST
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, "updatedAt").and(Sort.by(direction, "id")));
    }

    public static Pageable builderPageable(int page, int size) {
        validatePage(page, size);
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
    }

    private static void validatePage(int page, int size) {
        if (page < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La página no puede ser negativa");
        }
        if (size < 1 || size > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El tamaño de página debe estar entre 1 y 100");
        }
    }
}
