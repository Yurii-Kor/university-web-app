package ua.foxminded.university.web.student;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.foxminded.university.web.student.StudentsManagementWebMvcTestConstants.*;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ua.foxminded.university.service.student.StudentService;
import ua.foxminded.university.service.student.dto.StudentUpdateDto;
import ua.foxminded.university.service.student.exception.StudentUpdateException;
import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.student.cards.StudentsCardsPageModelFactory;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@WebMvcTest(controllers = StudentsManagementController.class)
@Import({
        StudentsManagementExceptionHandler.class,
        MethodSecurityTestConfig.class,
        ExceptionMessageReader.class
})
class StudentsManagementUpdateWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private StudyGroupService studyGroupService;
    
    @MockitoBean
    private TeacherService teacherService;

    @MockitoBean
    private StudentsCardsPageModelFactory pageFactory;

    @Test
    @DisplayName("POST /students/{id}/update as admin -> redirects to same page, flash ok/studentId, calls service")
    void updateStudent_admin_ok()
            throws Exception {

        mockMvc.perform(
                post("/students/{id}/update", STUDENT_ID)
                        .with(user(
                                Long.toString(ADMIN_ID))
                                .roles("ADMIN"))
                        .with(csrf())
                        .param(
                                "groupId",
                                Long.toString(GROUP_CS_ID))
                        .param(
                                "enrollmentYear",
                                Integer.toString(UPDATED_ENROLLMENT_YEAR))
                        .param(
                                "page",
                                "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/students?page=2"))
                .andExpect(flash().attribute(
                        "ok",
                        "Student updated."))
                .andExpect(flash().attribute(
                        "studentId",
                        STUDENT_ID));

        verify(studentService)
                .update(
                        STUDENT_ID,
                        new StudentUpdateDto(
                                GROUP_CS_ID,
                                UPDATED_ENROLLMENT_YEAR));
    }

    @Test
    @DisplayName("POST /students/{id}/update when target group is missing -> redirects, flash err/studentId")
    void updateStudent_missingGroup_redirectsWithCardError()
            throws Exception {

        var message =
                "StudyGroup not found: id="
                        + MISSING_GROUP_ID;

        var dto = new StudentUpdateDto(
                MISSING_GROUP_ID,
                UPDATED_ENROLLMENT_YEAR);

        when(studentService.update(
                STUDENT_ID,
                dto))
                .thenThrow(
                        new StudentUpdateException(
                                STUDENT_ID,
                                message));

        mockMvc.perform(
                post("/students/{id}/update", STUDENT_ID)
                        .with(user(
                                Long.toString(ADMIN_ID))
                                .roles("ADMIN"))
                        .with(csrf())
                        .param(
                                "groupId",
                                Long.toString(MISSING_GROUP_ID))
                        .param(
                                "enrollmentYear",
                                Integer.toString(UPDATED_ENROLLMENT_YEAR))
                        .param(
                                "page",
                                "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/students"))
                .andExpect(flash().attribute(
                        "err",
                        message))
                .andExpect(flash().attribute(
                        "studentId",
                        STUDENT_ID));

        verify(studentService)
                .update(
                        STUDENT_ID,
                        dto);
    }

    @Test
    @DisplayName("POST /students/{id}/update with missing group -> redirects to same page, flash validation data, service not called")
    void updateStudent_missingGroupId_redirectsWithCardError()
            throws Exception {

        mockMvc.perform(
                post("/students/{id}/update", STUDENT_ID)
                        .with(user(
                                Long.toString(ADMIN_ID))
                                .roles("ADMIN"))
                        .with(csrf())
                        .param(
                                "enrollmentYear",
                                Integer.toString(UPDATED_ENROLLMENT_YEAR))
                        .param(
                                "page",
                                "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/students?page=2"))
                .andExpect(flash().attributeExists(
                        "err"))
                .andExpect(flash().attribute(
                        "studentId",
                        STUDENT_ID))
                .andExpect(flash().attribute(
                        "requestedEnrollmentYear",
                        UPDATED_ENROLLMENT_YEAR));

        verify(studentService, never())
                .update(
                        anyLong(),
                        any());
    }

    @Test
    @DisplayName("POST /students/{id}/update with missing enrollment year -> redirects to same page, flash validation data, service not called")
    void updateStudent_missingEnrollmentYear_redirectsWithCardError()
            throws Exception {

        mockMvc.perform(
                post("/students/{id}/update", STUDENT_ID)
                        .with(user(
                                Long.toString(ADMIN_ID))
                                .roles("ADMIN"))
                        .with(csrf())
                        .param(
                                "groupId",
                                Long.toString(GROUP_CS_ID))
                        .param(
                                "page",
                                "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/students?page=2"))
                .andExpect(flash().attributeExists(
                        "err"))
                .andExpect(flash().attribute(
                        "studentId",
                        STUDENT_ID))
                .andExpect(flash().attribute(
                        "requestedGroupId",
                        GROUP_CS_ID));

        verify(studentService, never())
                .update(
                        anyLong(),
                        any());
    }

    @Test
    @DisplayName("POST /students/{id}/update when student not found -> redirects, flash err")
    void updateStudent_notFound_redirectsWithError()
            throws Exception {

        var message =
                "Student not found: id="
                        + STUDENT_ID;

        var dto = new StudentUpdateDto(
                GROUP_CS_ID,
                UPDATED_ENROLLMENT_YEAR);

        when(studentService.update(
                STUDENT_ID,
                dto))
                .thenThrow(
                        new EntityNotFoundException(
                                message));

        mockMvc.perform(
                post("/students/{id}/update", STUDENT_ID)
                        .with(user(
                                Long.toString(ADMIN_ID))
                                .roles("ADMIN"))
                        .with(csrf())
                        .param(
                                "groupId",
                                Long.toString(GROUP_CS_ID))
                        .param(
                                "enrollmentYear",
                                Integer.toString(UPDATED_ENROLLMENT_YEAR))
                        .param(
                                "page",
                                "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/students"))
                .andExpect(flash().attribute(
                        "err",
                        message));

        verify(studentService)
                .update(
                        STUDENT_ID,
                        dto);
    }

    @Test
    @DisplayName("POST /students/{id}/update as teacher -> 403, service not called")
    void updateStudent_teacher_forbidden()
            throws Exception {

        mockMvc.perform(
                post("/students/{id}/update", STUDENT_ID)
                        .with(user(
                                Long.toString(TEACHER_ID))
                                .roles("TEACHER"))
                        .with(csrf())
                        .param(
                                "groupId",
                                Long.toString(GROUP_CS_ID))
                        .param(
                                "enrollmentYear",
                                Integer.toString(UPDATED_ENROLLMENT_YEAR)))
                .andExpect(status().isForbidden());

        verify(studentService, never())
                .update(
                        anyLong(),
                        any());
    }

    @Test
    @DisplayName("POST /students/{id}/update as student -> 403, service not called")
    void updateStudent_student_forbidden()
            throws Exception {

        mockMvc.perform(
                post("/students/{id}/update", STUDENT_ID)
                        .with(user(
                                Long.toString(STUDENT_USER_ID))
                                .roles("STUDENT"))
                        .with(csrf())
                        .param(
                                "groupId",
                                Long.toString(GROUP_CS_ID))
                        .param(
                                "enrollmentYear",
                                Integer.toString(UPDATED_ENROLLMENT_YEAR)))
                .andExpect(status().isForbidden());

        verify(studentService, never())
                .update(
                        anyLong(),
                        any());
    }

    @Test
    @DisplayName("POST /students/{id}/update unauthenticated -> 401, service not called")
    void updateStudent_unauthenticated_unauthorized()
            throws Exception {

        mockMvc.perform(
                post("/students/{id}/update", STUDENT_ID)
                        .with(csrf())
                        .param(
                                "groupId",
                                Long.toString(GROUP_CS_ID))
                        .param(
                                "enrollmentYear",
                                Integer.toString(UPDATED_ENROLLMENT_YEAR)))
                .andExpect(status().isUnauthorized());

        verify(studentService, never())
                .update(
                        anyLong(),
                        any());
    }
}