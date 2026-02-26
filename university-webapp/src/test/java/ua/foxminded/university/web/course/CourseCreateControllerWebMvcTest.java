package ua.foxminded.university.web.course;

import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ua.foxminded.university.model.repository.dto.TeacherOptionView;
import ua.foxminded.university.service.CourseService;
import ua.foxminded.university.service.TeacherService;
import ua.foxminded.university.service.dto.request.course.CourseCreateDto;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;

@WebMvcTest(controllers = CourseCreateController.class)
@Import({ CourseCreateExceptionHandler.class, MethodSecurityTestConfig.class })
class CourseCreateControllerWebMvcTest {

    private static final Long USER_ID = 42L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CourseService courseService;

    @MockitoBean
    TeacherService teacherService;

    @Test
    @DisplayName("GET /courses/create as ADMIN -> 200, courses/create, model has form + teachers")
    void getCreateCourse_admin_ok_returnsCreateViewAndModel() throws Exception {
        var teachers = List.of(mock(TeacherOptionView.class));
        when(teacherService.listTeacherOptions()).thenReturn(teachers);

        mockMvc.perform(get("/courses/create")
                        .with(user(USER_ID.toString()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/create"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attribute("teachers", sameInstance(teachers)));
    }

    @Test
    @DisplayName("GET /courses/create as TEACHER -> 302 redirect (forbidden in this app), controller not invoked")
    void getCreateCourse_teacher_forbidden_redirects() throws Exception {
        mockMvc.perform(get("/courses/create")
                        .with(user(USER_ID.toString()).roles("TEACHER")))
                .andExpect(status().is3xxRedirection());

        verifyNoInteractions(courseService, teacherService);
    }

    @Test
    @DisplayName("POST /courses/create as ADMIN ok -> redirects /courses, flash ok, calls service")
    void postCreateCourse_admin_ok_redirectsAndSetsFlashAndCallsService() throws Exception {
        var teachers = List.of(mock(TeacherOptionView.class));
        when(teacherService.listTeacherOptions()).thenReturn(teachers);

        mockMvc.perform(post("/courses/create")
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("code", "CS-101")
                        .param("name", "Algorithms")
                        .param("description", "Intro")
                        .param("teacherId", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("ok", "Course created."));

        verify(courseService).createAll(List.of(
                new CourseCreateDto("CS-101", "Algorithms", "Intro", 5L)
        ));
    }

    @Test
    @DisplayName("POST /courses/create as ADMIN when validator gives violation -> 200, courses/create, field error, service not called")
    void postCreateCourse_validationViolation_staysOnFormAndNoServiceCall() throws Exception {
        var teachers = List.of(mock(TeacherOptionView.class));
        when(teacherService.listTeacherOptions()).thenReturn(teachers);

        @SuppressWarnings("unchecked")
        ConstraintViolation<CourseCreateDto> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("code");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");

        mockMvc.perform(post("/courses/create")
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("code", "")
                        .param("name", "Algorithms")
                        .param("description", "Intro")
                        .param("teacherId", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/create"))
                .andExpect(model().attributeHasFieldErrors("form", "code"))
                .andExpect(model().attributeExists("teachers"));

        verify(courseService, never()).createAll(any());
    }

    @Test
    @DisplayName("POST /courses/create as TEACHER -> 302 redirect (forbidden), service not called")
    void postCreateCourse_teacher_forbidden_redirects() throws Exception {
        mockMvc.perform(post("/courses/create")
                        .with(user(USER_ID.toString()).roles("TEACHER"))
                        .with(csrf())
                        .param("code", "CS-101")
                        .param("name", "Intro"))
                .andExpect(status().is3xxRedirection());

        verifyNoInteractions(courseService, teacherService);
    }

    @Test
    @DisplayName("POST /courses/create when service throws IllegalArgumentException -> redirects /courses/create, flash err + form restored (handler)")
    void postCreateCourse_illegalArgument_redirectsCreateAndSetsErrAndForm() throws Exception {
        doThrow(new IllegalArgumentException("Course names already exist: [algorithms]"))
                .when(courseService).createAll(any());

        mockMvc.perform(post("/courses/create")
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("code", "CS-101")
                        .param("name", "Algorithms")
                        .param("description", "Intro")
                        .param("teacherId", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/create"))
                .andExpect(flash().attribute("err", "Course names already exist: [algorithms]"))
                .andExpect(flash().attribute("form", new CourseCreateDto("CS-101", "Algorithms", "Intro", 5L)));
    }

    @Test
    @DisplayName("POST /courses/create when service throws ConstraintViolationException -> redirects /courses/create, flash err + form restored (handler)")
    void postCreateCourse_constraintViolation_redirectsCreateAndSetsErrAndForm() throws Exception {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("code");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("invalid");

        doThrow(new ConstraintViolationException(Set.of(violation)))
                .when(courseService).createAll(any());

        mockMvc.perform(post("/courses/create")
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("code", "CS-101")
                        .param("name", "Algorithms")
                        .param("description", "Intro")
                        .param("teacherId", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/create"))
                .andExpect(flash().attribute("err", "code: invalid"))
                .andExpect(flash().attribute("form", new CourseCreateDto("CS-101", "Algorithms", "Intro", 5L)));
    }

    @Test
    @DisplayName("POST /courses/create when service throws EntityNotFoundException -> redirects /courses/create, flash err + form restored (handler)")
    void postCreateCourse_entityNotFound_redirectsCreateAndSetsErrAndForm() throws Exception {
        doThrow(new EntityNotFoundException("Teacher not found: id=5"))
                .when(courseService).createAll(any());

        mockMvc.perform(post("/courses/create")
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("code", "CS-101")
                        .param("name", "Algorithms")
                        .param("description", "Intro")
                        .param("teacherId", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/create"))
                .andExpect(flash().attribute("err", "Teacher not found: id=5"))
                .andExpect(flash().attribute("form", new CourseCreateDto("CS-101", "Algorithms", "Intro", 5L)));
    }

    @Test
    @DisplayName("POST /courses/create when service throws DataIntegrityViolationException -> redirects /courses/create, flash err fixed (handler)")
    void postCreateCourse_dbConflict_redirectsCreateAndSetsFixedErrAndForm() throws Exception {
        doThrow(new DataIntegrityViolationException("conflict"))
                .when(courseService).createAll(any());

        mockMvc.perform(post("/courses/create")
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("code", "CS-101")
                        .param("name", "Algorithms")
                        .param("description", "Intro")
                        .param("teacherId", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/create"))
                .andExpect(flash().attribute("err", "Course code or name already exists."))
                .andExpect(flash().attribute("form", new CourseCreateDto("CS-101", "Algorithms", "Intro", 5L)));
    }

    @Test
    @DisplayName("POST /courses/create when service throws RuntimeException -> redirects /courses/create, flash generic err (handler)")
    void postCreateCourse_runtimeException_redirectsCreateAndSetsGenericErrAndForm() throws Exception {
        doThrow(new RuntimeException("boom"))
                .when(courseService).createAll(any());

        mockMvc.perform(post("/courses/create")
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("code", "CS-101")
                        .param("name", "Algorithms")
                        .param("description", "Intro")
                        .param("teacherId", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/create"))
                .andExpect(flash().attribute("err", "Something went wrong."))
                .andExpect(flash().attribute("form", new CourseCreateDto("CS-101", "Algorithms", "Intro", 5L)));
    }
}
