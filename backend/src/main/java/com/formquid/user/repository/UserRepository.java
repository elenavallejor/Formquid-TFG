package com.formquid.user.repository;

import com.formquid.user.dto.UserResponseDto;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Accede a usuarios.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(UserRole role);

    long countByRoleAndIdNot(UserRole role, UUID id);

    @Query(
        value = """
        select new com.formquid.user.dto.UserResponseDto(
            user.id,
            user.role,
            user.email,
            user.name,
            user.gender,
            user.institution,
            user.createdAt,
            user.updatedAt
        )
        from User user
        order by user.name asc
        """,
        countQuery = """
        select count(user)
        from User user
        """
    )
    Page<UserResponseDto> findResponses(Pageable pageable);

    @Query("""
        select new com.formquid.user.dto.UserResponseDto(
            user.id,
            user.role,
            user.email,
            user.name,
            user.gender,
            user.institution,
            user.createdAt,
            user.updatedAt
        )
        from User user
        where user.id = :userId
    """)
    Optional<UserResponseDto> findResponseById(@Param("userId") UUID userId);

    @Query("""
        select user.id
        from User user
        where user.role in :roles
    """)
    List<UUID> findIdsByRoleIn(@Param("roles") Collection<UserRole> roles);
}