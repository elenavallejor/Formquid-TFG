package com.formquid.tag.service;

import com.formquid.tag.dto.TagResponseDto;
import com.formquid.tag.entity.Tag;
import com.formquid.tag.repository.TagRepository;
import com.formquid.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Servicio de negocio para la gestión de etiquetas libres.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private static final int MAX_TAGS_PER_SURVEY = 10;
    private static final int MAX_TAG_LENGTH = 60;

    private final TagRepository tagRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Obtiene todas las etiquetas ordenadas alfabéticamente.
     */
    public List<TagResponseDto> findAll() {
        return tagRepository.findAllByOrderByNameAsc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Busca etiquetas por texto normalizado.
     */
    public List<TagResponseDto> search(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }

        String normalizedQuery = normalize(query);

        return tagRepository
            .findByNameNormalizedContainingOrderByNameAsc(normalizedQuery)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Obtiene o crea etiquetas a partir de una lista de nombres.
     *
     * Se utiliza desde el módulo de encuestas cuando un editor
     * crea o actualiza una encuesta.
     */
    @Transactional
    public Set<Tag> getOrCreateTags(
        List<String> names,
        Long creatorId
    ) {
        LinkedHashMap<String, String> cleanNames =
            cleanNames(names);

        if (cleanNames.isEmpty()) {
            return new LinkedHashSet<>();
        }

        List<Tag> existingTags =
            tagRepository.findByNameNormalizedIn(
                cleanNames.keySet()
            );

        Map<String, Tag> tagsByName = new HashMap<>();

        for (Tag tag : existingTags) {
            tagsByName.put(
                tag.getNameNormalized(),
                tag
            );
        }

        List<Tag> newTags = new ArrayList<>();

        for (
            Map.Entry<String, String> entry
                : cleanNames.entrySet()
        ) {
            String normalizedName = entry.getKey();
            String visibleName = entry.getValue();

            if (!tagsByName.containsKey(normalizedName)) {
                Tag tag = Tag.builder()
                    .name(visibleName)
                    .nameNormalized(normalizedName)
                    .createdBy(
                        creatorId != null
                            ? entityManager.getReference(
                                User.class,
                                creatorId
                            )
                            : null
                    )
                    .build();

                newTags.add(tag);

                tagsByName.put(
                    normalizedName,
                    tag
                );
            }
        }

        if (!newTags.isEmpty()) {
            tagRepository.saveAll(newTags);
        }

        LinkedHashSet<Tag> result =
            new LinkedHashSet<>();

        for (String normalizedName : cleanNames.keySet()) {
            result.add(
                tagsByName.get(normalizedName)
            );
        }

        return result;
    }

    /**
     * Limpia, normaliza y elimina nombres duplicados
     * conservando el orden original.
     */
    private LinkedHashMap<String, String> cleanNames(
        List<String> names
    ) {
        LinkedHashMap<String, String> result =
            new LinkedHashMap<>();

        if (names == null) {
            return result;
        }

        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }

            String cleanName = name.trim();

            if (cleanName.length() > MAX_TAG_LENGTH) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Una etiqueta no puede superar los 60 caracteres"
                );
            }

            result.putIfAbsent(
                normalize(cleanName),
                cleanName
            );
        }

        if (result.size() > MAX_TAGS_PER_SURVEY) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Una encuesta no puede tener más de 10 etiquetas"
            );
        }

        return result;
    }

    /**
     * Convierte una etiqueta a DTO.
     */
    private TagResponseDto toResponse(Tag tag) {
        return new TagResponseDto(
            tag.getId(),
            tag.getName()
        );
    }

    /**
     * Normaliza un nombre para realizar comparaciones
     * independientemente de mayúsculas y tildes.
     */
    private String normalize(String value) {
        String normalized = Normalizer.normalize(
                value.trim(),
                Normalizer.Form.NFD
            )
            .replaceAll("\\p{M}", "");

        return normalized.toLowerCase(Locale.ROOT);
    }
}