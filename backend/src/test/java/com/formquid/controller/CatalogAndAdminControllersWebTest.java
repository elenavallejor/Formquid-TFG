package com.formquid.controller;

import com.formquid.common.Category;
import com.formquid.common.CategoryController;
import com.formquid.common.dto.CategoryResponseDto;
import com.formquid.common.dto.CreateCategoryRequestDto;
import com.formquid.common.service.CategoryCatalogService;
import com.formquid.auth.security.JwtAuthenticationFilter;
import com.formquid.auth.security.JwtCookieService;
import com.formquid.auth.security.JwtService;
import com.formquid.block.controller.BlockController;
import com.formquid.block.service.BlockService;
import com.formquid.common.dto.CatalogSearchField;
import com.formquid.common.dto.CatalogSortOrder;
import com.formquid.config.SecurityConfig;
import com.formquid.dashboard.controller.AdminDashboardController;
import com.formquid.dashboard.service.AdminDashboardService;
import com.formquid.elementrequest.controller.ElementRequestAdminController;
import com.formquid.elementrequest.controller.ElementRequestController;
import com.formquid.elementrequest.service.ElementRequestService;
import com.formquid.notification.controller.NotificationController;
import com.formquid.notification.service.NotificationDestinationService;
import com.formquid.notification.service.NotificationService;
import com.formquid.question.controller.QuestionController;
import com.formquid.question.service.QuestionService;
import com.formquid.registration.controller.RegistrationRequestAdminController;
import com.formquid.registration.service.RegistrationRequestService;
import com.formquid.survey.controller.AdminSurveyController;
import com.formquid.survey.service.SurveyService;
import com.formquid.template.controller.TemplateController;
import com.formquid.template.service.TemplateService;
import com.formquid.user.controller.UserController;
import com.formquid.user.dto.UserResponseDto;
import com.formquid.user.dto.UserSearchField;
import com.formquid.user.dto.UserSortOrder;
import com.formquid.user.entity.User;
import com.formquid.user.entity.UserRole;
import com.formquid.user.repository.UserRepository;
import com.formquid.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
    BlockController.class,
    QuestionController.class,
    TemplateController.class,
    AdminDashboardController.class,
    ElementRequestController.class,
    ElementRequestAdminController.class,
    RegistrationRequestAdminController.class,
    AdminSurveyController.class,
    NotificationController.class,
    UserController.class,
    CategoryController.class
})
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class CatalogAndAdminControllersWebTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private BlockService blockService;
    @MockitoBean private QuestionService questionService;
    @MockitoBean private TemplateService templateService;
    @MockitoBean private AdminDashboardService dashboardService;
    @MockitoBean private ElementRequestService elementRequestService;
    @MockitoBean private RegistrationRequestService registrationRequestService;
    @MockitoBean private SurveyService surveyService;
    @MockitoBean private NotificationService notificationService;
    @MockitoBean private NotificationDestinationService notificationDestinationService;
    @MockitoBean private UserService userService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtCookieService jwtCookieService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private CategoryCatalogService categoryCatalogService;

    @Test
    void categoryCreation_requiresSuperadmin() throws Exception {
        when(categoryCatalogService.create(any(CreateCategoryRequestDto.class)))
            .thenReturn(new CategoryResponseDto("TECNOLOGIA_ASISTIVA", "Tecnología asistiva", false, "BsRobot"));

        mockMvc.perform(post("/api/v1/categories")
                .contentType("application/json")
                .content("{\"name\":\"Tecnología asistiva\",\"icon\":\"BsRobot\"}")
                .with(csrf())
                .with(authentication(tokenFor(user(2L, UserRole.ADMIN)))))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/categories")
                .contentType("application/json")
                .content("{\"name\":\"Tecnología asistiva\",\"icon\":\"BsRobot\"}")
                .with(csrf())
                .with(authentication(tokenFor(user(3L, UserRole.SUPERADMIN)))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("TECNOLOGIA_ASISTIVA"))
            .andExpect(jsonPath("$.icon").value("BsRobot"));
    }

    @Test
    void categoryIconUpdate_requiresSuperadminAndValidIcon() throws Exception {
        when(categoryCatalogService.updateIcon("TECNOLOGIA_ASISTIVA", "BsCpu"))
            .thenReturn(new CategoryResponseDto("TECNOLOGIA_ASISTIVA", "Tecnología asistiva", false, "BsCpu"));

        mockMvc.perform(patch("/api/v1/categories/TECNOLOGIA_ASISTIVA/icon")
                .contentType("application/json")
                .content("{\"icon\":\"BsCpu\"}")
                .with(csrf())
                .with(authentication(tokenFor(user(2L, UserRole.ADMIN)))))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/categories/TECNOLOGIA_ASISTIVA/icon")
                .contentType("application/json")
                .content("{\"icon\":\"invalid\"}")
                .with(csrf())
                .with(authentication(tokenFor(user(3L, UserRole.SUPERADMIN)))))
            .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v1/categories/TECNOLOGIA_ASISTIVA/icon")
                .contentType("application/json")
                .content("{\"icon\":\"BsCpu\"}")
                .with(csrf())
                .with(authentication(tokenFor(user(3L, UserRole.SUPERADMIN)))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.icon").value("BsCpu"));
    }

    @Test
    void catalogDetails_areAvailableToAuthenticatedEditor() throws Exception {
        User editor = user(1L, UserRole.EDITOR);

        mockMvc.perform(get("/api/v1/catalog/blocks/11").with(authentication(tokenFor(editor))))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/catalog/questions/12").with(authentication(tokenFor(editor))))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/catalog/templates/13").with(authentication(tokenFor(editor))))
            .andExpect(status().isOk());

        verify(blockService).findCatalogBlock(11L);
        verify(questionService).findFixedQuestion(12L);
        verify(templateService).findById(13L);
    }

    @Test
    void catalogList_acceptsSearchCategoriesAndSortOrder() throws Exception {
        User editor = user(1L, UserRole.EDITOR);

        mockMvc.perform(get("/api/v1/catalog/blocks")
                .param("page", "2")
                .param("size", "5")
                .param("mine", "true")
                .param("search", "infantil")
                .param("searchFields", "TITLE,CREATOR")
                .param("sortOrder", "OLDEST")
                .param("category", "DATOS_PERSONALES")
                .with(authentication(tokenFor(editor))))
            .andExpect(status().isOk());

        verify(blockService).findCatalogBlocks(
            2,
            5,
            editor,
            true,
            "infantil",
            Set.of(CatalogSearchField.TITLE, CatalogSearchField.CREATOR),
            CatalogSortOrder.OLDEST,
            Category.DATOS_PERSONALES
        );
    }

    @Test
    void adminDashboard_rejectsEditorAndAllowsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                .with(authentication(tokenFor(user(1L, UserRole.EDITOR)))))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/dashboard")
                .with(authentication(tokenFor(user(2L, UserRole.ADMIN)))))
            .andExpect(status().isOk());

        verify(dashboardService).getDashboard();
    }

    @Test
    void editor_canSubmitElementRequest() throws Exception {
        User editor = user(4L, UserRole.EDITOR);

        mockMvc.perform(post("/api/v1/element-requests")
                .with(authentication(tokenFor(editor)))
                .with(csrf())
                .contentType("application/json")
                .content("""
                    {"elementType":"BLOCK","message":"Nuevo bloque","requestPayload":{"name":"Bloque"}}
                    """))
            .andExpect(status().isCreated());

        verify(elementRequestService).create(any(), org.mockito.ArgumentMatchers.same(editor));
    }

    @Test
    void adminQueues_andSurveyAdministrationRequireAdminRole() throws Exception {
        var adminAuthentication = authentication(tokenFor(user(2L, UserRole.ADMIN)));

        mockMvc.perform(get("/api/v1/admin/element-requests").with(adminAuthentication))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/registration-requests")
                .with(authentication(tokenFor(user(2L, UserRole.ADMIN)))))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/surveys")
                .with(authentication(tokenFor(user(2L, UserRole.ADMIN)))))
            .andExpect(status().isOk());

        verify(elementRequestService).findPending();
        verify(registrationRequestService).findPending();
        verify(surveyService).findAdminSummaries(null, null, null, 0, 9);
    }

    @Test
    void notificationCount_usesAuthenticatedUserIdentity() throws Exception {
        User editor = user(8L, UserRole.EDITOR);
        when(notificationService.countUnread(8L)).thenReturn(3L);

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                .with(authentication(tokenFor(editor))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void profile_returnsCurrentDomainUser() throws Exception {
        User editor = user(8L, UserRole.EDITOR);
        when(userService.getProfile(editor)).thenReturn(new UserResponseDto(
            8L, UserRole.EDITOR, "user8@example.com", "Usuario",
            "Centro", "ES", Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T10:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/users/me").with(authentication(tokenFor(editor))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(8))
            .andExpect(jsonPath("$.role").value("EDITOR"))
            .andExpect(jsonPath("$.country").value("ES"))
            .andExpect(jsonPath("$.gender").doesNotExist());
    }

    @Test
    void userAdministration_requiresSuperadmin() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                .with(authentication(tokenFor(user(2L, UserRole.ADMIN)))))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/users")
                .with(authentication(tokenFor(user(3L, UserRole.SUPERADMIN)))))
            .andExpect(status().isOk());

        verify(userService).findAll(
            0,
            20,
            "",
            Set.of(
                UserSearchField.NAME,
                UserSearchField.EMAIL,
                UserSearchField.INSTITUTION,
                UserSearchField.COUNTRY,
                UserSearchField.ROLE
            ),
            UserSortOrder.NAME_ASC
        );
    }

    private User user(Long id, UserRole role) {
        return User.builder().id(id).email("user" + id + "@example.com").role(role).build();
    }

    private UsernamePasswordAuthenticationToken tokenFor(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }
}
