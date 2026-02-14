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
import jakarta.validation.ConstraintViolationException;

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
import ua.foxminded.university.service.dto.response.DeleteResult;
import ua.foxminded.university.service.dto.request.course.CourseDescriptionUpdateDto;
import ua.foxminded.university.service.dto.request.course.CourseSelfUpdateDto;
import ua.foxminded.university.web.course.dto.CourseFormMapper;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.PrincipalHandler;

@WebMvcTest(controllers = CoursesManagementController.class)
@Import({ CoursesManagementExceptionHandler.class, MethodSecurityTestConfig.class })
class CoursesManagementControllerWebMvcTest {

    private static final Long USER_ID = 42L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CourseService courseService;

    @MockitoBean
    TeacherService teacherService;

    @MockitoBean
    PrincipalHandler principalHandler;

    @MockitoBean
    CourseFormMapper mapper;

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
        var teachers = List.of(mock(TeacherOptionView.class));
        var courses = List.of(mock(CourseCardView.class));

        when(principalHandler.getRole(any())).thenReturn("student");
        when(principalHandler.parseUserId(any())).thenReturn(USER_ID);
        when(courseService.listCourseCardsForStudent(USER_ID)).thenReturn(courses);
        when(teacherService.listTeacherOptions()).thenReturn(teachers);

        mockMvc.perform(get("/courses").with(user(USER_ID.toString()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/courses"))
                .andExpect(model().attribute("pageTitle", "My courses"))
                .andExpect(model().attribute("pageSubtitle", "Courses assigned to your group."))
                .andExpect(model().attribute("courses", sameInstance(courses)));

        verify(courseService).listCourseCardsForStudent(USER_ID);
    }

    @Test
    @DisplayName("POST /courses/description/update as TEACHER ok -> redirects /courses, flash ok/courseId/courseOp, calls service")
    void postUpdateDescription_teacher_ok_redirectsAndSetsFlash() throws Exception {
        var dto = mock(CourseDescriptionUpdateDto.class);
        when(mapper.toDescriptionUpdateDto(any())).thenReturn(dto);

        mockMvc.perform(post("/courses/description/update")
                        .with(user(USER_ID.toString()).roles("TEACHER"))
                        .with(csrf())
                        .param("id", "10")
                        .param("description", "New desc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("ok", "Description updated."))
                .andExpect(flash().attribute("courseId", 10L))
                .andExpect(flash().attribute("courseOp", "description"));

        verify(courseService).updateDescription(dto);
    }

    @Test
    @DisplayName("POST /courses/self/update as ADMIN ok -> redirects /courses, flash ok/courseId/courseOp, calls service")
    void postUpdateSelf_admin_ok_redirectsAndSetsFlash() throws Exception {
        var dto = mock(CourseSelfUpdateDto.class);
        when(mapper.toSelfUpdateDto(any())).thenReturn(dto);

        mockMvc.perform(post("/courses/self/update")
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("id", "11")
                        .param("code", "CS-999")
                        .param("name", "X")
                        .param("teacherId", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("ok", "Course updated."))
                .andExpect(flash().attribute("courseId", 11L))
                .andExpect(flash().attribute("courseOp", "self"));

        verify(courseService).updateSelf(dto);
    }

    @Test
    @DisplayName("POST /courses/self/update as TEACHER -> redirect /courses, flash err=Access denied, service not called")
    void postUpdateSelf_teacher_forbidden_redirectsAndSetsErrFlash() throws Exception {

        mockMvc.perform(post("/courses/self/update")
                        .with(user("42").roles("TEACHER"))
                        .with(csrf())
                        .param("id", "10")
                        .param("code", "CS-101")
                        .param("name", "Some name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("err", "Access denied."))
                .andExpect(flash().attribute("courseId", 10L))
                .andExpect(flash().attribute("courseOp", "self"));

        verify(courseService, never()).updateSelf(any());
    }

    @Test
    @DisplayName("POST /courses/{id}/delete as ADMIN when deleted -> redirects /courses, flash ok + courseOp=delete")
    void postDeleteCourse_deleted_ok() throws Exception {
        var id = 50L;
        when(courseService.deleteByIds(List.of(id)))
                .thenReturn(new DeleteResult(Set.of(id), Set.of()));

        mockMvc.perform(post("/courses/{id}/delete", id)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("ok", "Course deleted."))
                .andExpect(flash().attribute("courseOp", "delete"));
    }

    @Test
    @DisplayName("POST /courses/{id}/delete as ADMIN when not found -> redirects /courses, flash err + courseOp=delete")
    void postDeleteCourse_notFound_err() throws Exception {
        var id = 51L;
        when(courseService.deleteByIds(List.of(id)))
                .thenReturn(new DeleteResult(Set.of(), Set.of(id)));

        mockMvc.perform(post("/courses/{id}/delete", id)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("err", "Course not found."))
                .andExpect(flash().attribute("courseOp", "delete"));
    }

    @Test
    @DisplayName("POST /courses/{id}/delete as TEACHER -> redirect /courses, flash err=Access denied, service not called")
    void postDeleteCourse_teacher_forbidden_redirectsAndSetsErrFlash() throws Exception {
        long courseId = 55L;

        mockMvc.perform(post("/courses/{id}/delete", courseId)
                        .with(user("42").roles("TEACHER"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("err", "Access denied."))
                .andExpect(flash().attribute("courseId", courseId))
                .andExpect(flash().attribute("courseOp", "delete"));

        verify(courseService, never()).deleteByIds(any());
    }


    @Test
    @DisplayName("POST /courses/self/update when ConstraintViolationException -> redirects /courses, flash err + courseId + courseOp=self (handler)")
    void postUpdateSelf_validationException_redirectsAndSetsErrFromHandler() throws Exception {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> v = mock(ConstraintViolation.class);
        Path path = mock(Path.class);

        when(path.toString()).thenReturn("code");
        when(v.getPropertyPath()).thenReturn(path);
        when(v.getMessage()).thenReturn("invalid");

        var ex = new ConstraintViolationException(Set.of(v));

        doThrow(ex).when(courseService).updateSelf(any());

        mockMvc.perform(post("/courses/self/update")
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("id", "77")
                        .param("code", "bad"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("err", "code: invalid"))
                .andExpect(flash().attribute("courseId", 77L))
                .andExpect(flash().attribute("courseOp", "self"));
    }

    @Test
    @DisplayName("POST /courses/{id}/delete when EntityNotFoundException -> redirects /courses, flash err + courseId + courseOp=delete (handler)")
    void postDeleteCourse_entityNotFound_redirectsAndSetsErrFromHandler() throws Exception {
        var id = 88L;
        doThrow(new EntityNotFoundException("Course not found: id=" + id))
                .when(courseService).deleteByIds(List.of(id));

        mockMvc.perform(post("/courses/{id}/delete", id)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("err", "Course not found: id=" + id))
                .andExpect(flash().attribute("courseId", id))
                .andExpect(flash().attribute("courseOp", "delete"));
    }
}
