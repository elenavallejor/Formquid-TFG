package com.formquid.block.repository;

import com.formquid.block.entity.Block;
import com.formquid.block.entity.BlockStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para bloques reutilizables.
 */
public interface BlockRepository extends JpaRepository<Block, Long> {

    /**
     * Busca un bloque por su nombre normalizado.
     *
     * @param nameNormalized nombre normalizado.
     * @return bloque encontrado, si existe.
     */
    Optional<Block> findByNameNormalized(String nameNormalized);

    /**
     * Comprueba si existe un bloque con el nombre normalizado indicado.
     *
     * @param nameNormalized nombre normalizado.
     * @return true si existe.
     */
    boolean existsByNameNormalized(String nameNormalized);

    /**
     * Obtiene todos los bloques ordenados por nombre.
     *
     * @return lista de bloques.
     */
    List<Block> findAllByOrderByNameAsc();

    /**
     * Obtiene bloques con un estado concreto ordenados por nombre.
     *
     * @param status estado de los bloques.
     * @return lista de bloques.
     */
    List<Block> findByStatusOrderByNameAsc(BlockStatus status);
}