package ua.foxminded.university.web.admin;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ua.foxminded.university.model.repository.dto.AdminRowView;
import ua.foxminded.university.service.AppUserService;
import ua.foxminded.university.service.TeacherService;
import ua.foxminded.university.service.dto.request.appuser.AppUserCreateDto;
import ua.foxminded.university.service.exception.appuser.AdminCreateException;
import ua.foxminded.university.web.admin.dto.AdminCreateForm;
import ua.foxminded.university.web.admin.dto.AdminCreateFormMapper;
import ua.foxminded.university.web.admin.validation.AdminCreateFormValidator;
import ua.foxminded.university.web.util.ExceptionMessageReader;
import ua.foxminded.university.web.util.PrincipalHandler;

@WebMvcTest(controllers = AdminManagementController.class)
@Import({ AdminExceptionHandler.class, PrincipalHandler.class, AdminCreateFormValidator.class, ExceptionMessageReader.class })
class AdminManagementControllerWebMvcTest {

    private static final Long SELF_ID = 42L;

    @Autowired
    MockMvc mockMvc;
    
    @MockitoBean
    TeacherService teacherService;

    @MockitoBean
    AppUserService appUserService;

    @MockitoBean
    AdminCreateFormMapper mapper;

    @Test
    @DisplayName("GET /admin -> 200, admin/admins, model has admins from service and selfId")
    void getAdmins_ok_returnsViewAndModel() throws Exception {
        var now = OffsetDateTime.parse("2025-01-01T12:00Z");

        var selfEnabled = new AdminRowView(SELF_ID, "self@ex.com", "Self", "Admin", now, true);
        var otherEnabled = new AdminRowView(10L, "a@ex.com", "A", "Admin", now.minusDays(1), true);
        var otherDisabled = new AdminRowView(11L, "b@ex.com", "B", "Admin", now.minusDays(2), false);

        when(appUserService.listAdminsForView(SELF_ID))
                .thenReturn(List.of(selfEnabled, otherEnabled, otherDisabled));

        mockMvc.perform(get("/admin").with(user(Long.toString(SELF_ID)).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admins"))
                .andExpect(model().attribute("selfId", SELF_ID))
                .andExpect(model().attribute("admins", contains(
                        selfEnabled,
                        otherEnabled,
                        otherDisabled
                )));
    }

    @Test
    @DisplayName("GET /admin/create -> 200, admin/create, model has empty form")
    void getCreateAdmin_ok_returnsCreateViewAndForm() throws Exception {
        mockMvc.perform(get("/admin/create").with(user(Long.toString(SELF_ID)).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/create"))
                .andExpect(model().attributeExists("form"));
    }

    @Test
    @DisplayName("POST /admin/create ok -> redirects /admin, flash created=true, calls service")
    void postCreateAdmin_ok_redirectsAndCallsService() throws Exception {
        var dto = new AppUserCreateDto("admin@ex.com", "Abcd1234!", "Alice", "Admin");

        when(mapper.toCreateDto(any(AdminCreateForm.class))).thenReturn(dto);

        mockMvc.perform(post("/admin/create")
                        .with(user(Long.toString(SELF_ID)).roles("ADMIN"))
                        .with(csrf())
                        .param("email", "admin@ex.com")
                        .param("firstName", "Alice")
                        .param("lastName", "Admin")
                        .param("newPassword", "Abcd1234!")
                        .param("confirmPassword", "Abcd1234!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("created", true));

        verify(appUserService).createAdmin(dto);
    }

    @Test
    @DisplayName("POST /admin/create password mismatch -> 200, admin/create, field error confirmPassword, service not called")
    void postCreateAdmin_passwordMismatch_staysOnForm() throws Exception {
        var dto = new AppUserCreateDto("admin@ex.com", "Abcd1234!", "Alice", "Admin");

        when(mapper.toCreateDto(any(AdminCreateForm.class))).thenReturn(dto);

        mockMvc.perform(post("/admin/create")
                        .with(user(Long.toString(SELF_ID)).roles("ADMIN"))
                        .with(csrf())
                        .param("email", "admin@ex.com")
                        .param("firstName", "Alice")
                        .param("lastName", "Admin")
                        .param("newPassword", "Abcd1234!")
                        .param("confirmPassword", "DIFFERENT!"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/create"))
                .andExpect(model().attributeHasFieldErrors("form", "confirmPassword"));

        verify(appUserService, never()).createAdmin(any());
    }

    @Test
    @DisplayName("POST /admin/create when service throws AdminCreateException -> redirects /admin/create, flash err=message")
    void postCreateAdmin_adminCreateException_redirectsCreateAndSetsErrFlash() throws Exception {
        var dto = new AppUserCreateDto("taken@ex.com", "Abcd1234!", "Alice", "Admin");

        when(mapper.toCreateDto(any(AdminCreateForm.class))).thenReturn(dto);
        doThrow(new AdminCreateException(dto, "Email already exists: taken@ex.com"))
                .when(appUserService).createAdmin(any());

        mockMvc.perform(post("/admin/create")
                        .with(user(Long.toString(SELF_ID)).roles("ADMIN"))
                        .with(csrf())
                        .param("email", "taken@ex.com")
                        .param("firstName", "Alice")
                        .param("lastName", "Admin")
                        .param("newPassword", "Abcd1234!")
                        .param("confirmPassword", "Abcd1234!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/create"))
                .andExpect(flash().attribute("err", "Email already exists: taken@ex.com"));
    }

    @Test
    @DisplayName("POST /admin/{id}/enable ok -> redirects /admin, flash ok, calls service")
    void postEnableAdmin_ok_redirectsAndCallsService() throws Exception {
        var targetId = 100L;

        mockMvc.perform(post("/admin/{id}/enable", targetId)
                        .with(user(Long.toString(SELF_ID)).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("ok", "Admin enabled."));

        verify(appUserService).enableUserByIds(targetId);
    }

    @Test
    @DisplayName("POST /admin/{id}/enable self -> redirects /admin, flash err (AdminExceptionHandler)")
    void postEnableAdmin_self_redirectsAdminAndSetsErr() throws Exception {
        mockMvc.perform(post("/admin/{id}/enable", SELF_ID)
                        .with(user(Long.toString(SELF_ID)).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("err", "You cannot modify your own admin account here."));

        verify(appUserService, never()).enableUserByIds(any());
    }

    @Test
    @DisplayName("POST /admin/{id}/disable ok -> redirects /admin, flash ok, calls service")
    void postDisableAdmin_ok_redirectsAndCallsService() throws Exception {
        var targetId = 101L;

        mockMvc.perform(post("/admin/{id}/disable", targetId)
                        .with(user(Long.toString(SELF_ID)).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("ok", "Admin disabled."));

        verify(appUserService).disableUserByIds(targetId);
    }

    @Test
    @DisplayName("POST /admin/{id}/delete when deleted -> redirects /admin and calls service")
    void postDeleteAdmin_deleted_redirectsAndCallsService() throws Exception {
        var targetId = 200L;

        mockMvc.perform(post("/admin/{id}/delete", targetId)
                        .with(user(Long.toString(SELF_ID)).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        verify(appUserService).deleteAdmin(targetId);
    }

    @Test
    @DisplayName("POST /admin/{id}/delete when EntityNotFoundException -> redirects /admin, flash err=message")
    void postDeleteAdmin_entityNotFound_redirectsAdminAndSetsErr() throws Exception {
        var targetId = 202L;

        doThrow(new EntityNotFoundException("Users not found: [" + targetId + "]"))
                .when(appUserService).deleteAdmin(targetId);

        mockMvc.perform(post("/admin/{id}/delete", targetId)
                        .with(user(Long.toString(SELF_ID)).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(flash().attribute("err", "Users not found: [" + targetId + "]"));
    }

    @Test
    @DisplayName("POST /admin/create when validator returns violation -> 200, admin/create, field error set from violation")
    void postCreateAdmin_validationViolation_bindsFieldError() throws Exception {
        var dto = new AppUserCreateDto("bad", "Abcd1234!", "Alice", "Admin");
        when(mapper.toCreateDto(any(AdminCreateForm.class))).thenReturn(dto);

        ConstraintViolation<AppUserCreateDto> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);

        when(path.toString()).thenReturn("email");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be a well-formed email address");

        mockMvc.perform(post("/admin/create")
                        .with(user(Long.toString(SELF_ID)).roles("ADMIN"))
                        .with(csrf())
                        .param("email", "bad")
                        .param("firstName", "Alice")
                        .param("lastName", "Admin")
                        .param("newPassword", "Abcd1234!")
                        .param("confirmPassword", "Abcd1234!"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/create"))
                .andExpect(model().attributeHasFieldErrors("form", "email"));

        verify(appUserService, never()).createAdmin(any());
    }
}
