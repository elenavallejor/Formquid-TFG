package com.formquid.block.repository;

import com.formquid.block.entity.Block;
import com.formquid.block.entity.BlockStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para bloques reutilizables.
 */
public interface BlockRepository extends JpaRepository<Block, Long> {

    /**
     * Busca un bloque por nombre.
     *
     */
    Optional<Block> findByNameIgnoreCase(String name);

    /**
     * Comprueba si existe un bloque con el nombre indicado ignorando
     * mayúsculas y minúsculas.
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Lista todos los bloques ordenados alfabéticamente.
     */
    List<Block> findAllByOrderByNameAsc();

    /**
     * Lista bloques filtrados por estado.
     */
    List<Block> findByStatusOrderByNameAsc(BlockStatus status);

    /**
     * Busca varios bloques por sus identificadores.
     *
     * Se usa al crear plantillas o encuestas a partir de bloques existentes.
     */
    List<Block> findByIdIn(Collection<Long> ids);
}