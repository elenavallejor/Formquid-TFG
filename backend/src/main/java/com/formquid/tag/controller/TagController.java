package com.formquid.tag.controller;

import com.formquid.tag.dto.TagResponseDto;
import com.formquid.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para consulta de etiquetas.
 *
 * La creación de etiquetas se realiza desde el módulo de encuestas.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TagController {

    private final TagService tagService;

    /**
     * Obtiene etiquetas disponibles para filtros públicos y sugerencias.
     *
     * @param query texto opcional de búsqueda.
     * @return lista de etiquetas.
     */
    @GetMapping("/public/tags")
    public List<TagResponseDto> findPublicTags(
        @RequestParam(required = false) String query
    ) {
        return tagService.search(query);
    }
}