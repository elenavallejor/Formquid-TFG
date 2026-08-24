package com.formquid.dashboard.controller;

import com.formquid.dashboard.dto.AdminDashboardResponseDto;
import com.formquid.dashboard.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping
    public AdminDashboardResponseDto getDashboard() {
        return dashboardService.getDashboard();
    }
}
