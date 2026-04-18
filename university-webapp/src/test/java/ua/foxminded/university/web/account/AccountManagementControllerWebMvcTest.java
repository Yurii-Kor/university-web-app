package ua.foxminded.university.web.account;

import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.OffsetDateTime;
import java.util.List;

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
import ua.foxminded.university.model.repository.dto.GroupView;
import ua.foxminded.university.model.repository.dto.StudentCardView;
import ua.foxminded.university.model.repository.dto.TeacherCardView;
import ua.foxminded.university.service.AppUserRoleTransitionService;
import ua.foxminded.university.service.AppUserService;
import ua.foxminded.university.service.StudentService;
import ua.foxminded.university.service.StudyGroupService;
import ua.foxminded.university.service.TeacherService;
import ua.foxminded.university.service.dto.request.appuser.StudentToTeacherRoleChangeDto;
import ua.foxminded.university.service.dto.request.appuser.TeacherToStudentRoleChangeDto;
import ua.foxminded.university.service.exception.appuser.StudentToTeacherRoleChangeException;
import ua.foxminded.university.service.exception.appuser.TeacherToStudentRoleChangeException;
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

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AccountsPageModelFactory pageFactory;

    @MockitoBean
    StudyGroupService studyGroupService;

    @MockitoBean
    AppUserRoleTransitionService roleTransitionService;

    @MockitoBean
    AppUserService appUserService;

    @MockitoBean
    StudentService studentService;

    @MockitoBean
    TeacherService teacherService;

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
    @DisplayName("POST /accounts/students/to-teacher ok -> redirects students page, flash ok + accountId, calls service")
    void postStudentToTeacher_ok_redirectsAndCallsService() throws Exception {
        var rank = anyRank();

        mockMvc.perform(post("/accounts/students/to-teacher")
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("userId", ACCOUNT_ID.toString())
                        .param("academicRank", rank.name())
                        .param("office", "T-301")
                        .param("page", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=students&page=2"))
                .andExpect(flash().attribute("ok", "Student role changed to teacher."))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID));

        verify(roleTransitionService).changeStudentToTeacher(
                new StudentToTeacherRoleChangeDto(ACCOUNT_ID, rank, "T-301")
        );
    }

    @Test
    @DisplayName("POST /accounts/teachers/to-student ok -> redirects teachers page, flash ok + accountId, calls service")
    void postTeacherToStudent_ok_redirectsAndCallsService() throws Exception {
        mockMvc.perform(post("/accounts/teachers/to-student")
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("userId", ACCOUNT_ID.toString())
                        .param("groupId", GROUP_ID.toString())
                        .param("enrollmentYear", "2024")
                        .param("page", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=teachers&page=1"))
                .andExpect(flash().attribute("ok", "Teacher role changed to student."))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID));

        verify(roleTransitionService).changeTeacherToStudent(
                new TeacherToStudentRoleChangeDto(ACCOUNT_ID, GROUP_ID, 2024)
        );
    }

    @Test
    @DisplayName("POST /accounts/students/to-teacher when StudentToTeacherRoleChangeException -> redirects students view, flash err + accountId")
    void postStudentToTeacher_roleChangeException_redirectsStudentsAndSetsErr() throws Exception {
        var rank = anyRank();
        var dto = new StudentToTeacherRoleChangeDto(ACCOUNT_ID, rank, "T-301");

        doThrow(new StudentToTeacherRoleChangeException(dto, "User is not a student: id=" + ACCOUNT_ID))
                .when(roleTransitionService).changeStudentToTeacher(any());

        mockMvc.perform(post("/accounts/students/to-teacher")
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("userId", ACCOUNT_ID.toString())
                        .param("academicRank", rank.name())
                        .param("office", "T-301")
                        .param("page", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=students"))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID))
                .andExpect(flash().attribute("err", "User is not a student: id=" + ACCOUNT_ID));
    }

    @Test
    @DisplayName("POST /accounts/teachers/to-student when TeacherToStudentRoleChangeException -> redirects teachers view, flash err + accountId")
    void postTeacherToStudent_roleChangeException_redirectsTeachersAndSetsErr() throws Exception {
        var dto = new TeacherToStudentRoleChangeDto(ACCOUNT_ID, GROUP_ID, 2024);

        doThrow(new TeacherToStudentRoleChangeException(dto, "Cannot change role: teacher has assigned courses (count=1)"))
                .when(roleTransitionService).changeTeacherToStudent(any());

        mockMvc.perform(post("/accounts/teachers/to-student")
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("userId", ACCOUNT_ID.toString())
                        .param("groupId", GROUP_ID.toString())
                        .param("enrollmentYear", "2024")
                        .param("page", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=teachers"))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID))
                .andExpect(flash().attribute("err", "Cannot change role: teacher has assigned courses (count=1)"));
    }

    @Test
    @DisplayName("POST /accounts/{id}/enable ok -> redirects current view/page, flash ok, calls service")
    void postEnableAccount_ok_redirectsAndCallsService() throws Exception {
        mockMvc.perform(post("/accounts/{id}/enable", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("view", "teachers")
                        .param("page", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=teachers&page=3"))
                .andExpect(flash().attribute("ok", "Account enabled."))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID));

        verify(appUserService).enableUserByIds(ACCOUNT_ID);
    }

    @Test
    @DisplayName("POST /accounts/{id}/disable ok -> redirects current view/page, flash ok, calls service")
    void postDisableAccount_ok_redirectsAndCallsService() throws Exception {
        mockMvc.perform(post("/accounts/{id}/disable", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("view", "students")
                        .param("page", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=students&page=1"))
                .andExpect(flash().attribute("ok", "Account disabled."))
                .andExpect(flash().attribute("accountId", ACCOUNT_ID));

        verify(appUserService).disableUserByIds(ACCOUNT_ID);
    }

    @Test
    @DisplayName("POST /accounts/{id}/delete with students view -> redirects students page, calls studentService")
    void postDeleteAccount_studentsView_redirectsAndCallsStudentService() throws Exception {
        mockMvc.perform(post("/accounts/{id}/delete", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("view", "students")
                        .param("page", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=students&page=2"))
                .andExpect(flash().attribute("ok", "Student account deleted."));

        verify(studentService).deleteByIds(List.of(ACCOUNT_ID));
        verify(teacherService, never()).deleteByIds(any());
    }

    @Test
    @DisplayName("POST /accounts/{id}/delete with teachers view -> redirects teachers page, calls teacherService")
    void postDeleteAccount_teachersView_redirectsAndCallsTeacherService() throws Exception {
        mockMvc.perform(post("/accounts/{id}/delete", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("view", "teachers")
                        .param("page", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts?view=teachers&page=4"))
                .andExpect(flash().attribute("ok", "Teacher account deleted."));

        verify(teacherService).deleteByIds(List.of(ACCOUNT_ID));
        verify(studentService, never()).deleteByIds(any());
    }

    @Test
    @DisplayName("POST /accounts/{id}/enable when EntityNotFoundException -> redirects /accounts, flash err")
    void postEnableAccount_entityNotFound_redirectsAccountsAndSetsErr() throws Exception {
        doThrow(new EntityNotFoundException("User not found: id=" + ACCOUNT_ID))
                .when(appUserService).enableUserByIds(ACCOUNT_ID);

        mockMvc.perform(post("/accounts/{id}/enable", ACCOUNT_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("view", "teachers")
                        .param("page", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts"))
                .andExpect(flash().attribute("err", "User not found: id=" + ACCOUNT_ID));
    }

    @Test
    @DisplayName("POST /accounts/teachers/to-student invalid dto -> redirects /accounts, flash err")
    void postTeacherToStudent_validationError_redirectsAccountsAndSetsErr() throws Exception {
        doThrow(mock(ConstraintViolationException.class))
                .when(roleTransitionService).changeTeacherToStudent(any());

        mockMvc.perform(post("/accounts/teachers/to-student")
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("userId", ACCOUNT_ID.toString())
                        .param("groupId", "")
                        .param("enrollmentYear", "")
                        .param("page", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/accounts"))
                .andExpect(flash().attributeExists("err"));
    }
}