package ua.foxminded.university.web.account;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.repository.dto.GroupView;
import ua.foxminded.university.model.repository.dto.StudentCardView;
import ua.foxminded.university.model.repository.dto.TeacherCardView;
import ua.foxminded.university.service.AppUserService;
import ua.foxminded.university.service.StudentService;
import ua.foxminded.university.service.StudyGroupService;
import ua.foxminded.university.service.TeacherService;
import ua.foxminded.university.service.dto.request.rolechange.ToStudentRoleChangeDto;
import ua.foxminded.university.service.dto.request.rolechange.ToTeacherRoleChangeDto;
import ua.foxminded.university.service.rolechange.RoleChangeAssessmentService;
import ua.foxminded.university.service.rolechange.RoleChangeService;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessment;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessmentMode;
import ua.foxminded.university.service.rolechange.exception.RoleChangeException;
import ua.foxminded.university.web.account.page.AccountsPage;
import ua.foxminded.university.web.account.page.AccountsPageModelFactory;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@WebMvcTest(controllers = AccountManagementController.class)
@Import({
        AccountManagementExceptionHandler.class,
        ExceptionMessageReader.class
})
class AccountManagementControllerWebMvcTest {

    private static final Long USER_ID = 42L;
    private static final Long ACCOUNT_ID = 101L;
    private static final Long GROUP_ID = 11L;

    @Autowired MockMvc mockMvc;

    @MockitoBean AccountsPageModelFactory pageFactory;
    @MockitoBean StudyGroupService studyGroupService;
    @MockitoBean RoleChangeAssessmentService roleChangeAssessmentService;
    @MockitoBean RoleChangeService roleChangeService;
    @MockitoBean AppUserService appUserService;
    @MockitoBean StudentService studentService;
    @MockitoBean TeacherService teacherService;

    private AcademicRank anyRank() {
        return AcademicRank.values()[0];
    }

    private OffsetDateTime fixedNow() {
        return OffsetDateTime.parse("2026-04-18T12:00:00Z");
    }

    private StudentCardView defaultStudentCard() {
        return new StudentCardView(
                1L,
                "student@example.com",
                "Alice",
                "Student",
                true,
                fixedNow(),
                2024,
                "QA-11"
        );
    }

    private TeacherCardView defaultTeacherCard() {
        return new TeacherCardView(
                2L,
                "teacher@example.com",
                "Bob",
                "Teacher",
                true,
                fixedNow(),
                anyRank(),
                "T-101"
        );
    }

    private AccountsPage defaultStudentsPage() {
        return new AccountsPage(
                "Account Management",
                "Manage sensitive operations for student accounts.",
                "students",
                0,
                2,
                false,
                true,
                List.of(defaultStudentCard()),
                List.of()
        );
    }

    private AccountsPage defaultTeachersPage() {
        return new AccountsPage(
                "Account Management",
                "Manage sensitive operations for teacher accounts.",
                "teachers",
                1,
                3,
                true,
                true,
                List.of(),
                List.of(defaultTeacherCard())
        );
    }

    private List<GroupView> defaultGroupOptions() {
        return List.of(
                new GroupView(1L, "QA-11"),
                new GroupView(2L, "SE-21")
        );
    }

    @Test
    @DisplayName("GET /accounts default students -> 200, accounts/accounts, model has page + groups + ranks")
    void getAccounts_defaultStudents_ok_returnsViewAndModel() throws Exception {
        var page = defaultStudentsPage();
        var groups = defaultGroupOptions();

        when(pageFactory.build("students", 0)).thenReturn(page);
        when(studyGroupService.listGroupOptions()).thenReturn(groups);

        mockMvc.perform(get("/accounts")
                        .with(user(USER_ID.toString()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("accounts/accounts"))
                .andExpect(model().attribute("page", page))
                .andExpect(model().attribute("all_groups", contains(
                        groups.get(0),
                        groups.get(1)
                )))
                .andExpect(model().attributeExists("all_ranks"));

        verify(pageFactory).build("students", 0);
        verify(studyGroupService).listGroupOptions();
    }

    @Test
    @DisplayName("GET /accounts?view=teachers&page=1 -> 200, accounts/accounts, teacher page model")
    void getAccounts_teachers_ok_returnsTeacherPage() throws Exception {
        var page = defaultTeachersPage();

        when(pageFactory.build("teachers", 1)).thenReturn(page);
        when(studyGroupService.listGroupOptions()).thenReturn(List.of());

        mockMvc.perform(get("/accounts")
                        .param("view", "teachers")
                        .param("page", "1")
                        .with(user(USER_ID.toString()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("accounts/accounts"))
                .andExpect(model().attribute("page", page))
                .andExpect(model().attributeExists("all_ranks"));

        verify(pageFactory).build("teachers", 1);
    }

    @Test
    @DisplayName("GET /accounts/{id}/role-change/assessment student -> teacher -> returns assessment")
    void getRoleChangeAssessment_studentToTeacher_ok_returnsAssessment() throws Exception {
        var assessment = new RoleChangeAssessment(
                ACCOUNT_ID,
                UserRole.STUDENT,
                UserRole.TEACHER,
                RoleChangeAssessmentMode.INPUT_REQUIRED,
                "Teacher profile data is required.",
                List.of("academicRank", "office")
        );

        when(roleChangeAssessmentService.assessRoleChange(ACCOUNT_ID, UserRole.STUDENT, UserRole.TEACHER))
                .thenReturn(assessment);

        mockMvc.perform(get("/accounts/{id}/role-change/assessment", ACCOUNT_ID)
                        .param("sourceRole", "STUDENT")
                        .param("targetRole", "TEACHER")
                        .with(user(USER_ID.toString()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(ACCOUNT_ID))
                .andExpect(jsonPath("$.sourceRole").value("STUDENT"))
                .andExpect(jsonPath("$.targetRole").value("TEACHER"))
                .andExpect(jsonPath("$.mode").value("INPUT_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Teacher profile data is required."));

        verify(roleChangeAssessmentService).assessRoleChange(ACCOUNT_ID, UserRole.STUDENT, UserRole.TEACHER);
    }

    @Test
    @DisplayName("GET /accounts/{id}/role-change/assessment teacher -> student -> returns assessment")
    void getRoleChangeAssessment_teacherToStudent_ok_returnsAssessment() throws Exception {
        var assessment = new RoleChangeAssessment(
                ACCOUNT_ID,
                UserRole.TEACHER,
                UserRole.STUDENT,
                RoleChangeAssessmentMode.AUTO_RESTORE_AVAILABLE,
                "Previous student profile found. It can be restored without additional data.",
                List.of()
        );

        when(roleChangeAssessmentService.assessRoleChange(ACCOUNT_ID, UserRole.TEACHER, UserRole.STUDENT))
                .thenReturn(assessment);

        mockMvc.perform(get("/accounts/{id}/role-change/assessment", ACCOUNT_ID)
                        .param("sourceRole", "TEACHER")
                        .param("targetRole", "STUDENT")
                        .with(user(USER_ID.toString()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(ACCOUNT_ID))
                .andExpect(jsonPath("$.sourceRole").value("TEACHER"))
                .andExpect(jsonPath("$.targetRole").value("STUDENT"))
                .andExpect(jsonPath("$.mode").value("AUTO_RESTORE_AVAILABLE"));

        verify(roleChangeAssessmentService).assessRoleChange(ACCOUNT_ID, UserRole.TEACHER, UserRole.STUDENT);
    }

    @Test
    @DisplayName("POST /accounts/{id}/role-change student -> teacher ok -> redirects teachers page, flash ok + accountId, calls service")
    void postChangeRole_studentToTeacher_ok_redirectsAndCallsService() throws Exception {
        var rank = anyRank();

        mockMvc.perform(post("/accounts/{id}/role-change", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("sourceRole", "STUDENT")
                        .param("targetRole", "TEACHER")
                        .param("academicRank", rank.name())
                        .param("office", "T-301"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=teachers&page=0"))
                .andExpect(flash().attribute("ok", "Student role changed to Teacher."))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID));

        verify(roleChangeService).changeRole(
                eq(ACCOUNT_ID),
                eq(UserRole.STUDENT),
                eq(UserRole.TEACHER),
                eq(new ToTeacherRoleChangeDto(rank, "T-301"))
        );
    }

    @Test
    @DisplayName("POST /accounts/{id}/role-change/restore student -> teacher ok -> redirects teachers page, flash ok + accountId, calls service")
    void postRestoreRole_studentToTeacher_ok_redirectsAndCallsService() throws Exception {
        mockMvc.perform(post("/accounts/{id}/role-change/restore", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("sourceRole", "STUDENT")
                        .param("targetRole", "TEACHER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=teachers&page=0"))
                .andExpect(flash().attribute("ok", "Student role changed to Teacher."))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID));

        verify(roleChangeService).restoreRole(ACCOUNT_ID, UserRole.STUDENT, UserRole.TEACHER);
    }

    @Test
    @DisplayName("POST /accounts/{id}/role-change teacher -> student ok -> redirects students page, flash ok + accountId, calls service")
    void postChangeRole_teacherToStudent_ok_redirectsAndCallsService() throws Exception {
        mockMvc.perform(post("/accounts/{id}/role-change", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("sourceRole", "TEACHER")
                        .param("targetRole", "STUDENT")
                        .param("groupId", GROUP_ID.toString())
                        .param("enrollmentYear", "2024"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=students&page=0"))
                .andExpect(flash().attribute("ok", "Teacher role changed to Student."))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID));

        verify(roleChangeService).changeRole(
                eq(ACCOUNT_ID),
                eq(UserRole.TEACHER),
                eq(UserRole.STUDENT),
                eq(new ToStudentRoleChangeDto(GROUP_ID, 2024))
        );
    }

    @Test
    @DisplayName("POST /accounts/{id}/role-change/restore teacher -> student ok -> redirects students page, flash ok + accountId, calls service")
    void postRestoreRole_teacherToStudent_ok_redirectsAndCallsService() throws Exception {
        mockMvc.perform(post("/accounts/{id}/role-change/restore", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("sourceRole", "TEACHER")
                        .param("targetRole", "STUDENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=students&page=0"))
                .andExpect(flash().attribute("ok", "Teacher role changed to Student."))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID));

        verify(roleChangeService).restoreRole(ACCOUNT_ID, UserRole.TEACHER, UserRole.STUDENT);
    }

    @Test
    @DisplayName("POST /accounts/{id}/role-change student -> teacher when RoleChangeException -> redirects source students view, flash err + accountId")
    void postChangeRole_studentToTeacher_roleChangeException_redirectsStudentsAndSetsErr() throws Exception {
        var rank = anyRank();

        doThrow(new RoleChangeException(
                ACCOUNT_ID,
                UserRole.STUDENT,
                UserRole.TEACHER,
                "User is not a student: id=" + ACCOUNT_ID
        )).when(roleChangeService).changeRole(
                eq(ACCOUNT_ID),
                eq(UserRole.STUDENT),
                eq(UserRole.TEACHER),
                any()
        );

        mockMvc.perform(post("/accounts/{id}/role-change", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("sourceRole", "STUDENT")
                        .param("targetRole", "TEACHER")
                        .param("academicRank", rank.name())
                        .param("office", "T-301"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=students&page=0"))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID))
                .andExpect(flash().attribute("err", "User is not a student: id=" + ACCOUNT_ID));
    }

    @Test
    @DisplayName("POST /accounts/{id}/role-change teacher -> student when RoleChangeException -> redirects source teachers view, flash err + accountId")
    void postChangeRole_teacherToStudent_roleChangeException_redirectsTeachersAndSetsErr() throws Exception {
        doThrow(new RoleChangeException(
                ACCOUNT_ID,
                UserRole.TEACHER,
                UserRole.STUDENT,
                "Cannot change role: teacher has assigned courses (count=1)"
        )).when(roleChangeService).changeRole(
                eq(ACCOUNT_ID),
                eq(UserRole.TEACHER),
                eq(UserRole.STUDENT),
                any()
        );

        mockMvc.perform(post("/accounts/{id}/role-change", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("sourceRole", "TEACHER")
                        .param("targetRole", "STUDENT")
                        .param("groupId", GROUP_ID.toString())
                        .param("enrollmentYear", "2024"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=teachers&page=0"))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID))
                .andExpect(flash().attribute("err", "Cannot change role: teacher has assigned courses (count=1)"));
    }

    @Test
    @DisplayName("POST /accounts/{id}/enable teacher ok -> redirects teachers page, flash ok, calls service")
    void postEnableAccount_teacher_ok_redirectsAndCallsService() throws Exception {
        mockMvc.perform(post("/accounts/{id}/enable", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("role", "TEACHER")
                        .param("page", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=teachers&page=3"))
                .andExpect(flash().attribute("ok", "Teacher account enabled."))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID));

        verify(appUserService).enableUserByIds(ACCOUNT_ID);
    }

    @Test
    @DisplayName("POST /accounts/{id}/enable when EntityNotFoundException -> redirects /accounts, flash err")
    void postEnableAccount_entityNotFound_redirectsAccountsAndSetsErr() throws Exception {
        doThrow(new EntityNotFoundException("User not found: id=" + ACCOUNT_ID))
                .when(appUserService).enableUserByIds(ACCOUNT_ID);

        mockMvc.perform(post("/accounts/{id}/enable", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("role", "TEACHER")
                        .param("page", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts"))
                .andExpect(flash().attribute("err", "User not found: id=" + ACCOUNT_ID));
    }

    @Test
    @DisplayName("POST /accounts/{id}/disable student ok -> redirects students page, flash ok, calls service")
    void postDisableAccount_student_ok_redirectsAndCallsService() throws Exception {
        mockMvc.perform(post("/accounts/{id}/disable", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("role", "STUDENT")
                        .param("page", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=students&page=1"))
                .andExpect(flash().attribute("ok", "Student account disabled."))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID));

        verify(appUserService).disableUserByIds(ACCOUNT_ID);
    }

    @Test
    @DisplayName("POST /accounts/{id}/delete student -> redirects students page, calls studentService")
    void postDeleteAccount_student_redirectsAndCallsStudentService() throws Exception {
        mockMvc.perform(post("/accounts/{id}/delete", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("role", "STUDENT")
                        .param("page", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=students&page=2"))
                .andExpect(flash().attribute("ok", "Student account deleted."))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID));

        verify(studentService).deleteById(ACCOUNT_ID);
        verify(teacherService, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("POST /accounts/{id}/delete teacher -> redirects teachers page, calls teacherService")
    void postDeleteAccount_teacher_redirectsAndCallsTeacherService() throws Exception {
        mockMvc.perform(post("/accounts/{id}/delete", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("role", "TEACHER")
                        .param("page", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=teachers&page=4"))
                .andExpect(flash().attribute("ok", "Teacher account deleted."))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID));

        verify(teacherService).deleteById(ACCOUNT_ID);
        verify(studentService, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("POST /accounts/{id}/role-change teacher -> student invalid dto -> redirects /accounts, flash err")
    void postChangeRole_teacherToStudent_validationError_redirectsAccountsAndSetsErr() throws Exception {
        doThrow(new ConstraintViolationException(Set.of()))
                .when(roleChangeService).changeRole(
                        eq(ACCOUNT_ID),
                        eq(UserRole.TEACHER),
                        eq(UserRole.STUDENT),
                        any()
                );

        mockMvc.perform(post("/accounts/{id}/role-change", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("sourceRole", "TEACHER")
                        .param("targetRole", "STUDENT")
                        .param("groupId", "")
                        .param("enrollmentYear", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts"))
                .andExpect(flash().attributeExists("err"));
    }
}