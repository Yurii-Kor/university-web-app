package ua.foxminded.university.web.account;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.service.appuser.AppUserService;
import ua.foxminded.university.service.rolechange.RoleChangeFacade;
import ua.foxminded.university.service.student.StudentService;
import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.account.delete.AccountDeleterRegistry;
import ua.foxminded.university.web.account.delete.strategy.AccountDeleter;
import ua.foxminded.university.web.account.page.AccountsPageModelFactory;
import ua.foxminded.university.web.util.ExceptionMessageReader;
import ua.foxminded.university.security.config.SecurityConfig;
import ua.foxminded.university.security.web.LoginAuthEntryPoint;
import ua.foxminded.university.security.web.LoginFailureHandler;
import ua.foxminded.university.security.web.LogoutToLoginSuccessHandler;
import ua.foxminded.university.security.web.ProfileAccessDeniedHandler;

@WebMvcTest(controllers = AccountManagementController.class)
@Import({
        SecurityConfig.class,
        AccountManagementExceptionHandler.class,
        ExceptionMessageReader.class,
        ProfileAccessDeniedHandler.class
})
class AccountManagementControllerSecurityWebMvcTest {

    static final Long USER_ID = 42L;
    static final Long ACCOUNT_ID = 101L;
    static final Long GROUP_ID = 11L;

    @Autowired MockMvc mockMvc;

    @MockitoBean AccountsPageModelFactory pageFactory;
    @MockitoBean StudyGroupService studyGroupService;
    @MockitoBean RoleChangeFacade roleChangeFacade;
    @MockitoBean AppUserService appUserService;
    @MockitoBean StudentService studentService;
    @MockitoBean TeacherService teacherService;
    @MockitoBean AccountDeleterRegistry accountDeleterRegistry;
    @MockitoBean AccountDeleter accountDeleter;

    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean PasswordEncoder passwordEncoder;
    @MockitoBean LoginFailureHandler loginFailureHandler;
    @MockitoBean LoginAuthEntryPoint loginAuthEntryPoint;
    @MockitoBean LogoutToLoginSuccessHandler logoutSuccessHandler;

    @ParameterizedTest(name = "{0} cannot access account management endpoint")
    @MethodSource("nonAdminRequests")
    @DisplayName("Account management endpoints are protected from non-admin users")
    void accountManagementEndpoints_nonAdminUser_accessDenied(String role,
                                                              MockHttpServletRequestBuilder request) throws Exception {

        mockMvc.perform(request.with(user(USER_ID.toString()).roles(role)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts"))
                .andExpect(flash().attribute("err", "Access denied."));

        verifyNoInteractions(pageFactory);
        verifyNoInteractions(studyGroupService);
        verifyNoInteractions(roleChangeFacade);
        verifyNoInteractions(appUserService);
        verifyNoInteractions(accountDeleterRegistry);
        verifyNoInteractions(accountDeleter);
    }

    private static Stream<Arguments> nonAdminRequests() {
        return Stream.of("STUDENT", "TEACHER")
                .flatMap(role -> securedAccountRequests()
                        .map(request -> Arguments.of(role, request)));
    }

    private static Stream<MockHttpServletRequestBuilder> securedAccountRequests() {
        return Stream.of(
                get("/accounts"),

                get("/accounts/{id}/role-change/assessment", ACCOUNT_ID)
                        .param("sourceRole", "STUDENT")
                        .param("targetRole", "TEACHER"),

                post("/accounts/{id}/role-change", ACCOUNT_ID)
                        .with(csrf())
                        .param("sourceRole", "STUDENT")
                        .param("targetRole", "TEACHER")
                        .param("academicRank", AcademicRank.values()[0].name())
                        .param("office", "T-301"),

                post("/accounts/{id}/role-change", ACCOUNT_ID)
                        .with(csrf())
                        .param("sourceRole", "TEACHER")
                        .param("targetRole", "STUDENT")
                        .param("groupId", GROUP_ID.toString())
                        .param("enrollmentYear", "2024"),

                post("/accounts/{id}/role-change/restore", ACCOUNT_ID)
                        .with(csrf())
                        .param("sourceRole", "STUDENT")
                        .param("targetRole", "TEACHER"),

                post("/accounts/{id}/enable", ACCOUNT_ID)
                        .with(csrf())
                        .param("role", "TEACHER")
                        .param("page", "0"),

                post("/accounts/{id}/disable", ACCOUNT_ID)
                        .with(csrf())
                        .param("role", "STUDENT")
                        .param("page", "0"),

                post("/accounts/{id}/delete", ACCOUNT_ID)
                        .with(csrf())
                        .param("role", "STUDENT")
                        .param("page", "0")
        );
    }
}