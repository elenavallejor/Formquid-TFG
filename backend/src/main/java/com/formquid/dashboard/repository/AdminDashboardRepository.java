package com.formquid.dashboard.repository;

import com.formquid.dashboard.dto.AdminDashboardCountsDto;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminDashboardRepository {

    private final EntityManager entityManager;

    /**
     * Obtiene los tres contadores del dashboard
     */
    public AdminDashboardCountsDto getCounts() {
        Object[] row = (Object[]) entityManager.createNativeQuery("""
            select
                (select count(*) from element_requests),
                (select count(*) from surveys),
                (select count(*) from registration_requests)
        """).getSingleResult();

        return new AdminDashboardCountsDto(
            ((Number) row[0]).longValue(),
            ((Number) row[1]).longValue(),
            ((Number) row[2]).longValue()
        );
    }
}