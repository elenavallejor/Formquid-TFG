package com.formquid.user.repository;

import com.formquid.user.dto.UserResponseDto;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Accede a usuarios y proporciona consultas optimizadas para su administración.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query(value = "select exists (select 1 from users)", nativeQuery = true)
    boolean existsAny();

    boolean existsByRole(UserRole role);

    long countByRoleAndIdNot(UserRole role, Long id);

    @Query(value = "select user_id from superadmin_guard where singleton for update", nativeQuery = true)
    Long lockSuperadminGuard();

    @Query(
        value = """
            select new com.formquid.user.dto.UserResponseDto(
                user.id,
                user.role,
                user.email,
                user.name,
                user.institution,
                user.country,
                user.profileImageKey,
                user.createdAt,
                user.updatedAt
            )
            from User user
            where :search = ''
                or (:searchName = true and lower(user.name) like concat('%', :search, '%'))
                or (:searchEmail = true and lower(user.email) like concat('%', :search, '%'))
                or (:searchInstitution = true and lower(user.institution) like concat('%', :search, '%'))
                or (:searchCountry = true and (lower(user.country) like concat('%', :search, '%') or user.country in :countryCodes))
                or (
                    :searchRole = true
                    and lower(case
                        when user.role = com.formquid.user.entity.UserRole.EDITOR then 'editor'
                        when user.role = com.formquid.user.entity.UserRole.ADMIN then 'administrador admin'
                        else 'superadministrador superadmin'
                    end) like concat('%', :search, '%')
                )
        """,
        countQuery = """
            select count(user)
            from User user
            where :search = ''
                or (:searchName = true and lower(user.name) like concat('%', :search, '%'))
                or (:searchEmail = true and lower(user.email) like concat('%', :search, '%'))
                or (:searchInstitution = true and lower(user.institution) like concat('%', :search, '%'))
                or (:searchCountry = true and (lower(user.country) like concat('%', :search, '%') or user.country in :countryCodes))
                or (
                    :searchRole = true
                    and lower(case
                        when user.role = com.formquid.user.entity.UserRole.EDITOR then 'editor'
                        when user.role = com.formquid.user.entity.UserRole.ADMIN then 'administrador admin'
                        else 'superadministrador superadmin'
                    end) like concat('%', :search, '%')
                )
        """
    )
    Page<UserResponseDto> findResponses(
        @Param("search") String search,
        @Param("searchName") boolean searchName,
        @Param("searchEmail") boolean searchEmail,
        @Param("searchInstitution") boolean searchInstitution,
        @Param("searchCountry") boolean searchCountry,
        @Param("searchRole") boolean searchRole,
        @Param("countryCodes") Collection<String> countryCodes,
        Pageable pageable
    );

    @Query("""
        select new com.formquid.user.dto.UserResponseDto(
            user.id,
            user.role,
            user.email,
            user.name,
            user.institution,
            user.country,
            user.profileImageKey,
            user.createdAt,
            user.updatedAt
        )
        from User user
        where user.id = :userId
    """)
    Optional<UserResponseDto> findResponseById(@Param("userId") Long userId);

    @Query("""
        select user.id
        from User user
        where user.role in :roles
    """)
    List<Long> findIdsByRoleIn(@Param("roles") Collection<UserRole> roles);
}
