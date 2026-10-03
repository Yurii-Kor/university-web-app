package ua.foxminded.university.web.profile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ua.foxminded.university.service.appuser.AppUserService;
import ua.foxminded.university.service.appuser.dto.AppUserPasswordChangeDto;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.profile.validation.PasswordChangeFormValidator;
import ua.foxminded.university.web.util.ExceptionMessageReader;
import ua.foxminded.university.web.util.PrincipalHandler;

@WebMvcTest(controllers = PasswordChangeController.class)
@Import({
        PasswordChangeExceptionHandler.class,
        PasswordChangeFormValidator.class,
        PrincipalHandler.class,
        ExceptionMessageReader.class
})
class PasswordChangeControllerWebMvcTest {

    private static final Long DEFAULT_ID = 42L;

    private static final String CURRENT_PASSWORD = "Abcd1234!";
    private static final String NEW_PASSWORD = "Wxyz1234!";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AppUserService appUserService;
    
    @MockitoBean
    TeacherService teacherService;

    @Test
    @DisplayName("GET /profile/password -> 200, profile/password")
    void getPasswordPage_ok_returnsPasswordView() throws Exception {

        mockMvc.perform(
                        get("/profile/password")
                                .with(user(
                                        Long.toString(DEFAULT_ID))
                                        .roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/password"));
    }

    @Test
    @DisplayName("POST /profile/password -> changes password, redirects /profile, flash passwordUpdated=true")
    void postChangePassword_ok_redirectsProfileAndCallsService()
            throws Exception {

        mockMvc.perform(
                        post("/profile/password")
                                .with(user(
                                        Long.toString(DEFAULT_ID))
                                        .roles("TEACHER"))
                                .with(csrf())
                                .param(
                                        "currentPassword",
                                        CURRENT_PASSWORD)
                                .param(
                                        "newPassword",
                                        NEW_PASSWORD)
                                .param(
                                        "confirmPassword",
                                        NEW_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(
                        flash().attribute(
                                "passwordUpdated",
                                true));

        verify(appUserService)
                .changePasswordSelf(
                        DEFAULT_ID,
                        new AppUserPasswordChangeDto(
                                CURRENT_PASSWORD,
                                NEW_PASSWORD,
                                NEW_PASSWORD));
    }

    @Test
    @DisplayName("POST /profile/password with mismatched confirmation -> redirects back, flash err, service not called")
    void postChangePassword_mismatch_redirectsBackAndDoesNotCallService()
            throws Exception {

        mockMvc.perform(
                        post("/profile/password")
                                .with(user(
                                        Long.toString(DEFAULT_ID))
                                        .roles("TEACHER"))
                                .with(csrf())
                                .param(
                                        "currentPassword",
                                        CURRENT_PASSWORD)
                                .param(
                                        "newPassword",
                                        NEW_PASSWORD)
                                .param(
                                        "confirmPassword",
                                        "Different123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/profile/password"))
                .andExpect(
                        flash().attribute(
                                "err",
                                "Passwords do not match."));

        verify(
                appUserService,
                never())
                .changePasswordSelf(
                        anyLong(),
                        any(AppUserPasswordChangeDto.class));
    }

    @Test
    @DisplayName("POST /profile/password with invalid new password -> redirects back with validation error, service not called")
    void postChangePassword_invalidNewPassword_redirectsBackAndDoesNotCallService()
            throws Exception {

        mockMvc.perform(
                        post("/profile/password")
                                .with(user(
                                        Long.toString(DEFAULT_ID))
                                        .roles("TEACHER"))
                                .with(csrf())
                                .param(
                                        "currentPassword",
                                        CURRENT_PASSWORD)
                                .param(
                                        "newPassword",
                                        "bad")
                                .param(
                                        "confirmPassword",
                                        "bad"))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/profile/password"))
                .andExpect(
                        flash().attributeExists(
                                "err"));

        verify(
                appUserService,
                never())
                .changePasswordSelf(
                        anyLong(),
                        any(AppUserPasswordChangeDto.class));
    }

    @Test
    @DisplayName("POST /profile/password with wrong current password -> redirects back, flash err")
    void postChangePassword_wrongCurrent_redirectsBackAndSetsErrFlash()
            throws Exception {

        doThrow(
                new IllegalArgumentException(
                        "Current password is incorrect"))
                .when(appUserService)
                .changePasswordSelf(
                        eq(DEFAULT_ID.longValue()),
                        any(AppUserPasswordChangeDto.class));

        mockMvc.perform(
                        post("/profile/password")
                                .with(user(
                                        Long.toString(DEFAULT_ID))
                                        .roles("TEACHER"))
                                .with(csrf())
                                .param(
                                        "currentPassword",
                                        CURRENT_PASSWORD)
                                .param(
                                        "newPassword",
                                        NEW_PASSWORD)
                                .param(
                                        "confirmPassword",
                                        NEW_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/profile/password"))
                .andExpect(
                        flash().attribute(
                                "err",
                                "Current password is incorrect"));
    }

    @Test
    @DisplayName("POST /profile/password when user not found -> redirects /login, flash fixed error")
    void postChangePassword_userNotFound_redirectsLoginAndSetsErrFlash()
            throws Exception {

        doThrow(
                new EntityNotFoundException(
                        "not found"))
                .when(appUserService)
                .changePasswordSelf(
                        eq(DEFAULT_ID.longValue()),
                        any(AppUserPasswordChangeDto.class));

        mockMvc.perform(
                        post("/profile/password")
                                .with(user(
                                        Long.toString(DEFAULT_ID))
                                        .roles("TEACHER"))
                                .with(csrf())
                                .param(
                                        "currentPassword",
                                        CURRENT_PASSWORD)
                                .param(
                                        "newPassword",
                                        NEW_PASSWORD)
                                .param(
                                        "confirmPassword",
                                        NEW_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl("/login"))
                .andExpect(
                        flash().attribute(
                                "err",
                                "User not found. Please log in again."));
    }

    @Test
    @DisplayName("POST /profile/password when runtime exception -> redirects back, flash generic error")
    void postChangePassword_runtimeException_redirectsBackAndSetsGenericErrFlash()
            throws Exception {

        doThrow(
                new RuntimeException("boom"))
                .when(appUserService)
                .changePasswordSelf(
                        eq(DEFAULT_ID.longValue()),
                        any(AppUserPasswordChangeDto.class));

        mockMvc.perform(
                        post("/profile/password")
                                .with(user(
                                        Long.toString(DEFAULT_ID))
                                        .roles("TEACHER"))
                                .with(csrf())
                                .param(
                                        "currentPassword",
                                        CURRENT_PASSWORD)
                                .param(
                                        "newPassword",
                                        NEW_PASSWORD)
                                .param(
                                        "confirmPassword",
                                        NEW_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/profile/password"))
                .andExpect(
                        flash().attribute(
                                "err",
                                "Something went wrong."));
    }
}