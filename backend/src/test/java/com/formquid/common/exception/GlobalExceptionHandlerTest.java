package com.formquid.common.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    @Test
    void delete_whenDatabaseRejectsLastSuperadmin_shouldReturnConflict() throws Exception {
        mvc("ck_users_require_superadmin").perform(delete("/users/1"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("status").value(409))
            .andExpect(jsonPath("message").value("Debe existir siempre un superadministrador"));
    }

    @Test
    void delete_whenUnrelatedConstraintFails_shouldKeepGenericErrorWithoutDatabaseDetails() throws Exception {
        mvc("unrelated_constraint").perform(delete("/users/1"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("message").value("Ha ocurrido un error interno"));
    }

    private MockMvc mvc(String constraint) {
        return MockMvcBuilders.standaloneSetup(new FailingController(constraint))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @RestController
    static class FailingController {
        private final String constraint;

        FailingController(String constraint) {
            this.constraint = constraint;
        }

        @DeleteMapping("/users/{id}")
        void deleteUser() {
            throw new DataIntegrityViolationException("Database details",
                new ConstraintViolationException("Database details",
                    new SQLException("Database details", "23514"), constraint));
        }
    }
}
