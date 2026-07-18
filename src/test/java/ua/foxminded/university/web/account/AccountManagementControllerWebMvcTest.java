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

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.persistence.student.projection.StudentCardView;
import ua.foxminded.university.model.persistence.studygroup.projection.GroupView;
import ua.foxminded.university.model.persistence.teacher.projection.TeacherCardView;
import ua.foxminded.university.service.appuser.AppUserService;
import ua.foxminded.university.service.rolechange.RoleChangeFacade;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessment;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessmentMode;
import ua.foxminded.university.service.rolechange.exception.RoleChangeException;
import ua.foxminded.university.service.rolechange.target.strategy.data.ToStudentRoleProfileData;
import ua.foxminded.university.service.rolechange.target.strategy.data.ToTeacherRoleProfileData;
import ua.foxminded.university.service.student.StudentService;
import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.account.delete.AccountDeleterRegistry;
import ua.foxminded.university.web.account.delete.strategy.AccountDeleter;
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
    @MockitoBean RoleChangeFacade roleChangeFacade;
    @MockitoBean AppUserService appUserService;
    @MockitoBean StudentService studentService;
    @MockitoBean TeacherService teacherService;
    @MockitoBean AccountDeleterRegistry accountDeleterRegistry;
    @MockitoBean AccountDeleter accountDeleter;

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

        when(roleChangeFacade.assessRoleChange(ACCOUNT_ID, UserRole.STUDENT, UserRole.TEACHER))
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

        verify(roleChangeFacade).assessRoleChange(ACCOUNT_ID, UserRole.STUDENT, UserRole.TEACHER);
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

        when(roleChangeFacade.assessRoleChange(ACCOUNT_ID, UserRole.TEACHER, UserRole.STUDENT))
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

        verify(roleChangeFacade).assessRoleChange(ACCOUNT_ID, UserRole.TEACHER, UserRole.STUDENT);
    }

    @Test
    @DisplayName("POST /accounts/{id}/role-change student -> teacher ok -> redirects teachers page, flash ok + accountId, calls facade")
    void postChangeRole_studentToTeacher_ok_redirectsAndCallsFacade() throws Exception {
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

        verify(roleChangeFacade).changeRoleToTeacher(
                eq(ACCOUNT_ID),
                eq(UserRole.STUDENT),
                argThat(form -> form instanceof ToTeacherRoleProfileData teacherForm
                        && teacherForm.academicRank() == rank
                        && "T-301".equals(teacherForm.office()))
        );
    }

    @Test
    @DisplayName("POST /accounts/{id}/role-change teacher -> student ok -> redirects students page, flash ok + accountId, calls facade")
    void postChangeRole_teacherToStudent_ok_redirectsAndCallsFacade() throws Exception {
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

        verify(roleChangeFacade).changeRoleToStudent(
                eq(ACCOUNT_ID),
                eq(UserRole.TEACHER),
                argThat(form -> form instanceof ToStudentRoleProfileData studentForm
                        && GROUP_ID.equals(studentForm.groupId())
                        && Integer.valueOf(2024).equals(studentForm.enrollmentYear()))
        );
    }

    @Test
    @DisplayName("POST /accounts/{id}/role-change/restore student -> teacher ok -> redirects teachers page, flash ok + accountId, calls facade")
    void postRestoreRole_studentToTeacher_ok_redirectsAndCallsFacade() throws Exception {
        mockMvc.perform(post("/accounts/{id}/role-change/restore", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("sourceRole", "STUDENT")
                        .param("targetRole", "TEACHER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=teachers&page=0"))
                .andExpect(flash().attribute("ok", "Student role changed to Teacher."))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID));

        verify(roleChangeFacade).restoreRole(ACCOUNT_ID, UserRole.STUDENT, UserRole.TEACHER);
    }

    @Test
    @DisplayName("POST /accounts/{id}/role-change/restore teacher -> student ok -> redirects students page, flash ok + accountId, calls facade")
    void postRestoreRole_teacherToStudent_ok_redirectsAndCallsFacade() throws Exception {
        mockMvc.perform(post("/accounts/{id}/role-change/restore", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("sourceRole", "TEACHER")
                        .param("targetRole", "STUDENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=students&page=0"))
                .andExpect(flash().attribute("ok", "Teacher role changed to Student."))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID));

        verify(roleChangeFacade).restoreRole(ACCOUNT_ID, UserRole.TEACHER, UserRole.STUDENT);
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
        )).when(roleChangeFacade).changeRoleToTeacher(
                eq(ACCOUNT_ID),
                eq(UserRole.STUDENT),
                any(ToTeacherRoleProfileData.class)
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
        )).when(roleChangeFacade).changeRoleToStudent(
                eq(ACCOUNT_ID),
                eq(UserRole.TEACHER),
                any(ToStudentRoleProfileData.class)
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
    @DisplayName("POST /accounts/{id}/role-change target admin -> redirects accounts page, flash err")
    void postChangeRole_targetAdmin_redirectsAccountsAndSetsErr() throws Exception {
        mockMvc.perform(post("/accounts/{id}/role-change", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("sourceRole", "STUDENT")
                        .param("targetRole", "ADMIN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts"))
                .andExpect(flash().attribute(
                        "err",
                        "Changing account role to ADMIN is not supported from this workflow."
                ));

        verify(roleChangeFacade, never()).changeRoleToStudent(anyLong(), any(), any());
        verify(roleChangeFacade, never()).changeRoleToTeacher(anyLong(), any(), any());
    }

    @Test
    @DisplayName("POST /accounts/{id}/role-change teacher -> student invalid form -> redirects source teachers page, flash err")
    void postChangeRole_teacherToStudent_validationError_redirectsSourceViewAndSetsErr() throws Exception {
        mockMvc.perform(post("/accounts/{id}/role-change", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("sourceRole", "TEACHER")
                        .param("targetRole", "STUDENT")
                        .param("groupId", "")
                        .param("enrollmentYear", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=teachers&page=0"))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID))
                .andExpect(flash().attribute("err", "Role change form contains invalid data."));

        verify(roleChangeFacade, never()).changeRoleToStudent(anyLong(), any(), any());
        verify(roleChangeFacade, never()).changeRoleToTeacher(anyLong(), any(), any());
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
    @DisplayName("POST /accounts/{id}/delete student -> redirects students page, calls student deleter")
    void postDeleteAccount_student_redirectsAndCallsStudentDeleter() throws Exception {
        when(accountDeleterRegistry.getRequired(UserRole.STUDENT))
                .thenReturn(accountDeleter);

        mockMvc.perform(post("/accounts/{id}/delete", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("role", "STUDENT")
                        .param("page", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=students&page=2"))
                .andExpect(flash().attribute("ok", "Student account deleted."))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID));

        verify(accountDeleterRegistry).getRequired(UserRole.STUDENT);
        verify(accountDeleter).deleteByRoleAndId(UserRole.STUDENT, ACCOUNT_ID);
    }

    @Test
    @DisplayName("POST /accounts/{id}/delete teacher -> redirects teachers page, calls teacher deleter")
    void postDeleteAccount_teacher_redirectsAndCallsTeacherDeleter() throws Exception {
        when(accountDeleterRegistry.getRequired(UserRole.TEACHER))
                .thenReturn(accountDeleter);

        mockMvc.perform(post("/accounts/{id}/delete", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("role", "TEACHER")
                        .param("page", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=teachers&page=4"))
                .andExpect(flash().attribute("ok", "Teacher account deleted."))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID));

        verify(accountDeleterRegistry).getRequired(UserRole.TEACHER);
        verify(accountDeleter).deleteByRoleAndId(UserRole.TEACHER, ACCOUNT_ID);
    }
}