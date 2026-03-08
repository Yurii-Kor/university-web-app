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
import jakarta.validation.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ua.foxminded.university.model.repository.dto.CourseCardView;
import ua.foxminded.university.model.repository.dto.TeacherOptionView;
import ua.foxminded.university.service.CourseService;
import ua.foxminded.university.service.TeacherService;
import ua.foxminded.university.service.dto.request.course.CourseDescriptionUpdateDto;
import ua.foxminded.university.service.dto.request.course.CourseSelfUpdateDto;
import ua.foxminded.university.service.util.validation.ContextConstraintViolationException;
import ua.foxminded.university.web.course.page.CoursesPageModelFactory;
import ua.foxminded.university.web.course.page.strategy.AdminCoursesPageStrategy;
import ua.foxminded.university.web.course.page.strategy.StudentCoursesPageStrategy;
import ua.foxminded.university.web.course.page.strategy.TeacherCoursesPageStrategy;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.PrincipalHandler;

@WebMvcTest(controllers = CoursesManagementController.class)
@Import({
    CoursesManagementExceptionHandler.class,
    MethodSecurityTestConfig.class,
    CoursesPageModelFactory.class,
    AdminCoursesPageStrategy.class,
    TeacherCoursesPageStrategy.class,
    StudentCoursesPageStrategy.class
})
class CoursesManagementControllerWebMvcTest {

    private static final Long USER_ID = 42L;
    private static final Long COURSE_ID = 42L;

    @Autowired MockMvc mockMvc;

    @MockitoBean CourseService courseService;
    @MockitoBean TeacherService teacherService;
    @MockitoBean PrincipalHandler principalHandler;

    @Test
    @DisplayName("GET /courses as admin -> 200, courses/courses, model has title/subtitle/courses/teachers")
    void getCourses_admin_ok_returnsAdminModel() throws Exception {
        var teachers = List.of(mock(TeacherOptionView.class));
        var courses = List.of(mock(CourseCardView.class), mock(CourseCardView.class));

        when(principalHandler.getRole(any())).thenReturn("admin");
        when(principalHandler.parseUserId(any())).thenReturn(USER_ID);
        when(courseService.listCourseCardsForAdmin()).thenReturn(courses);
        when(teacherService.listTeacherOptions()).thenReturn(teachers);

        mockMvc.perform(get("/courses").with(user(USER_ID.toString()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/courses"))
                .andExpect(model().attribute("pageTitle", "Courses"))
                .andExpect(model().attribute("pageSubtitle", "All courses in the system."))
                .andExpect(model().attribute("courses", sameInstance(courses)))
                .andExpect(model().attribute("teachers", sameInstance(teachers)));
    }

    @Test
    @DisplayName("GET /courses as teacher -> 200, courses/courses, model has My courses, calls listCourseCardsForTeacher(userId)")
    void getCourses_teacher_ok_returnsTeacherModel() throws Exception {
        var teachers = List.of(mock(TeacherOptionView.class));
        var courses = List.of(mock(CourseCardView.class));

        when(principalHandler.getRole(any())).thenReturn("teacher");
        when(principalHandler.parseUserId(any())).thenReturn(USER_ID);
        when(courseService.listCourseCardsForTeacher(USER_ID)).thenReturn(courses);
        when(teacherService.listTeacherOptions()).thenReturn(teachers);

        mockMvc.perform(get("/courses").with(user(USER_ID.toString()).roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/courses"))
                .andExpect(model().attribute("pageTitle", "My courses"))
                .andExpect(model().attribute("pageSubtitle", "Courses you teach."))
                .andExpect(model().attribute("courses", sameInstance(courses)))
                .andExpect(model().attribute("teachers", sameInstance(teachers)));

        verify(courseService).listCourseCardsForTeacher(USER_ID);
    }

    @Test
    @DisplayName("GET /courses as student -> 200, courses/courses, calls listCourseCardsForStudent(userId)")
    void getCourses_student_ok_returnsStudentModel() throws Exception {
        var courses = List.of(mock(CourseCardView.class));

        when(principalHandler.getRole(any())).thenReturn("student");
        when(principalHandler.parseUserId(any())).thenReturn(USER_ID);
        when(courseService.listCourseCardsForStudent(USER_ID)).thenReturn(courses);

        mockMvc.perform(get("/courses").with(user(USER_ID.toString()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/courses"))
                .andExpect(model().attribute("pageTitle", "My courses"))
                .andExpect(model().attribute("pageSubtitle", "Courses assigned to your group."))
                .andExpect(model().attribute("courses", sameInstance(courses)));

        verify(courseService).listCourseCardsForStudent(USER_ID);
    }

    @Test
    @DisplayName("POST /courses/description/update as TEACHER ok -> redirects /courses, flash ok/courseId, calls service")
    void postUpdateDescription_teacher_ok_redirectsAndSetsFlash() throws Exception {
        mockMvc.perform(post("/courses/description/update")
                        .with(user(USER_ID.toString()).roles("TEACHER"))
                        .with(csrf())
                        .param("id", COURSE_ID.toString())
                        .param("description", "New desc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("ok", "Description updated."))
                .andExpect(flash().attribute("courseId", COURSE_ID));

        verify(courseService).updateDescription(new CourseDescriptionUpdateDto(COURSE_ID, "New desc"));
    }

    @Test
    @DisplayName("POST /courses/self/update as ADMIN ok -> redirects /courses, flash ok/courseId, calls service")
    void postUpdateSelf_admin_ok_redirectsAndSetsFlash() throws Exception {
        mockMvc.perform(post("/courses/self/update")
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("id", COURSE_ID.toString())
                        .param("code", "CS-999")
                        .param("name", "X")
                        .param("teacherId", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("ok", "Course updated."))
                .andExpect(flash().attribute("courseId", COURSE_ID));

        verify(courseService).updateSelf(new CourseSelfUpdateDto(COURSE_ID, "CS-999", "X", 5L));
    }

    @Test
    @DisplayName("POST /courses/self/update as TEACHER -> redirect /courses, flash err=Access denied, service not called")
    void postUpdateSelf_teacher_forbidden_redirectsAndSetsErrFlash() throws Exception {
        mockMvc.perform(post("/courses/self/update")
                        .with(user(USER_ID.toString()).roles("TEACHER"))
                        .with(csrf())
                        .param("id", COURSE_ID.toString())
                        .param("code", "CS-101")
                        .param("name", "Some name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("err", "Access denied."));

        verify(courseService, never()).updateSelf(any());
    }

    @Test
    @DisplayName("POST /courses/{id}/delete as ADMIN -> redirects /courses, flash ok, calls service.delete(id)")
    void postDeleteCourse_deleted_ok() throws Exception {
        doNothing().when(courseService).delete(COURSE_ID);

        mockMvc.perform(post("/courses/{id}/delete", COURSE_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("ok", "Course deleted."));

        verify(courseService).delete(COURSE_ID);
    }

    @Test
    @DisplayName("POST /courses/{id}/delete as ADMIN when not found -> redirects /courses, flash err from handler")
    void postDeleteCourse_notFound_err() throws Exception {
        doThrow(new EntityNotFoundException("Course not found: id=" + COURSE_ID))
                .when(courseService).delete(COURSE_ID);

        mockMvc.perform(post("/courses/{id}/delete", COURSE_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("err", "Course not found: id=" + COURSE_ID));

        verify(courseService).delete(COURSE_ID);
    }

    @Test
    @DisplayName("POST /courses/{id}/delete as TEACHER -> redirect /courses, flash err=Access denied, service not called")
    void postDeleteCourse_teacher_forbidden_redirectsAndSetsErrFlash() throws Exception {
        mockMvc.perform(post("/courses/{id}/delete", COURSE_ID)
                        .with(user(USER_ID.toString()).roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("err", "Access denied."));

        verify(courseService, never()).delete(anyLong());
    }


    @Test
    @DisplayName("POST /courses/self/update when ContextConstraintViolationException -> redirects /courses, flash err + courseId (handler)")
    void postUpdateSelf_validationException_redirectsAndSetsErrFromHandler() throws Exception {
        ConstraintViolation<Object> violations = mock(ConstraintViolation.class);
        Path path = mock(Path.class);

        when(path.toString()).thenReturn("code");
        when(violations.getPropertyPath()).thenReturn(path);
        when(violations.getMessage()).thenReturn("invalid");

		var ex = new ContextConstraintViolationException(COURSE_ID, Set.of(violations));

        doThrow(ex).when(courseService).updateSelf(any());

        mockMvc.perform(post("/courses/self/update")
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("id", COURSE_ID.toString())
                        .param("code", "bad"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("err", "code: invalid"))
                .andExpect(flash().attribute("courseId", COURSE_ID));
    }

    @Test
    @DisplayName("POST /courses/{id}/delete when EntityNotFoundException -> redirects /courses, flash err (handler)")
    void postDeleteCourse_entityNotFound_redirectsAndSetsErrFromHandler() throws Exception {
        doThrow(new EntityNotFoundException("Course not found: id=" + COURSE_ID))
                .when(courseService).delete(COURSE_ID);

        mockMvc.perform(post("/courses/{id}/delete", COURSE_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("err", "Course not found: id=" + COURSE_ID));
    }
}
