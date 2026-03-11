package ua.foxminded.university.web.course;

import static org.junit.jupiter.api.Assertions.*;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ua.foxminded.university.model.repository.dto.CourseCardView;
import ua.foxminded.university.service.CourseService;
import ua.foxminded.university.service.TeacherService;
import ua.foxminded.university.service.dto.request.course.CourseDescriptionUpdateDto;
import ua.foxminded.university.service.dto.request.course.CourseSelfUpdateDto;
import ua.foxminded.university.service.util.validation.ContextConstraintViolationException;
import ua.foxminded.university.web.course.page.CoursesPageModel;
import ua.foxminded.university.web.course.page.CoursesPageModelFactory;
import ua.foxminded.university.web.course.page.strategy.AdminCoursesPageStrategy;
import ua.foxminded.university.web.course.page.strategy.StudentCoursesPageStrategy;
import ua.foxminded.university.web.course.page.strategy.TeacherCoursesPageStrategy;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;
import ua.foxminded.university.web.util.PrincipalHandler;

@WebMvcTest(controllers = CoursesManagementController.class)
@Import({
    CoursesManagementExceptionHandler.class,
    MethodSecurityTestConfig.class,
    CoursesPageModelFactory.class,
    AdminCoursesPageStrategy.class,
    TeacherCoursesPageStrategy.class,
    StudentCoursesPageStrategy.class,
    ExceptionMessageReader.class
})
class CoursesManagementControllerWebMvcTest {

    private static final Long USER_ID = 42L;
    private static final Long COURSE_ID = 42L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CourseService courseService;

    @MockitoBean
    TeacherService teacherService;

    @MockitoBean
    PrincipalHandler principalHandler;

    private CoursesPageModel extractPage(MvcResult result) {
        var model = result.getModelAndView().getModel();
        assertTrue(model.containsKey("page"), "model must contain 'page'");
        return (CoursesPageModel) model.get("page");
    }

    @Test
    @DisplayName("GET /courses as admin -> 200, courses/courses, model has admin page")
    void getCourses_admin_ok_returnsAdminModel() throws Exception {
        var courses = List.of(mock(CourseCardView.class), mock(CourseCardView.class));
        var coursesPage = new PageImpl<>(courses, PageRequest.of(0, 6), 7);

        when(principalHandler.getRole(any())).thenReturn("admin");
        when(principalHandler.parseUserId(any())).thenReturn(USER_ID);
        when(courseService.listCourseCardsForAdmin(any(Pageable.class))).thenReturn(coursesPage);

        var result = mockMvc.perform(get("/courses")
                        .with(user(USER_ID.toString()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/courses"))
                .andExpect(model().attributeExists("page"))
                .andReturn();

        var page = extractPage(result);

        assertEquals("Courses", page.pageTitle());
        assertEquals("All courses in the system.", page.pageSubtitle());
        assertIterableEquals(courses, page.courses());
        assertTrue(page.showTeacherInfo());
        assertTrue(page.allowDescriptionEdit());
        assertTrue(page.showAdminActions());
        assertTrue(page.showCreateButton());
        assertEquals(0, page.currentPage());
        assertEquals(2, page.totalPages());
        assertFalse(page.hasPrevious());
        assertTrue(page.hasNext());

        verify(courseService).listCourseCardsForAdmin(argThat(p -> p.getPageNumber() == 0));
    }

    @Test
    @DisplayName("GET /courses as teacher -> 200, courses/courses, model has teacher page")
    void getCourses_teacher_ok_returnsTeacherModel() throws Exception {
        var courses = List.of(mock(CourseCardView.class));
        var coursesPage = new PageImpl<>(courses, PageRequest.of(1, 6), 7);

        when(principalHandler.getRole(any())).thenReturn("teacher");
        when(principalHandler.parseUserId(any())).thenReturn(USER_ID);
        when(courseService.listCourseCardsForTeacher(eq(USER_ID), any(Pageable.class))).thenReturn(coursesPage);

        var result = mockMvc.perform(get("/courses")
                        .param("page", "1")
                        .with(user(USER_ID.toString()).roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/courses"))
                .andExpect(model().attributeExists("page"))
                .andReturn();

        var page = extractPage(result);

        assertEquals("My courses", page.pageTitle());
        assertEquals("Courses you teach.", page.pageSubtitle());
        assertIterableEquals(courses, page.courses());
        assertFalse(page.showTeacherInfo());
        assertTrue(page.allowDescriptionEdit());
        assertFalse(page.showAdminActions());
        assertFalse(page.showCreateButton());
        assertEquals(1, page.currentPage());
        assertEquals(2, page.totalPages());
        assertTrue(page.hasPrevious());
        assertFalse(page.hasNext());

        verify(courseService).listCourseCardsForTeacher(eq(USER_ID), argThat(p -> p.getPageNumber() == 1));
    }

    @Test
    @DisplayName("GET /courses as student -> 200, courses/courses, model has student page")
    void getCourses_student_ok_returnsStudentModel() throws Exception {
        var courses = List.of(mock(CourseCardView.class));
        var coursesPage = new PageImpl<>(courses, PageRequest.of(0, 6), 1);

        when(principalHandler.getRole(any())).thenReturn("student");
        when(principalHandler.parseUserId(any())).thenReturn(USER_ID);
        when(courseService.listCourseCardsForStudent(eq(USER_ID), any(Pageable.class))).thenReturn(coursesPage);

        var result = mockMvc.perform(get("/courses")
                        .with(user(USER_ID.toString()).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/courses"))
                .andExpect(model().attributeExists("page"))
                .andReturn();

        var page = extractPage(result);

        assertEquals("My courses", page.pageTitle());
        assertEquals("Courses assigned to your group.", page.pageSubtitle());
        assertIterableEquals(courses, page.courses());
        assertTrue(page.showTeacherInfo());
        assertFalse(page.allowDescriptionEdit());
        assertFalse(page.showAdminActions());
        assertFalse(page.showCreateButton());
        assertEquals(0, page.currentPage());
        assertEquals(1, page.totalPages());
        assertFalse(page.hasPrevious());
        assertFalse(page.hasNext());

        verify(courseService).listCourseCardsForStudent(eq(USER_ID), argThat(p -> p.getPageNumber() == 0));
    }

    @Test
    @DisplayName("POST /courses/description/update as TEACHER ok -> redirects /courses?page=1, flash ok/courseId, calls service")
    void postUpdateDescription_teacher_ok_redirectsAndSetsFlash() throws Exception {
        mockMvc.perform(post("/courses/description/update")
                        .with(user(USER_ID.toString()).roles("TEACHER"))
                        .with(csrf())
                        .param("id", COURSE_ID.toString())
                        .param("description", "New desc")
                        .param("page", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses?page=1"))
                .andExpect(flash().attribute("ok", "Description updated."))
                .andExpect(flash().attribute("courseId", COURSE_ID));

        verify(courseService).updateDescription(new CourseDescriptionUpdateDto(COURSE_ID, "New desc"));
    }

    @Test
    @DisplayName("POST /courses/self/update as ADMIN ok -> redirects /courses?page=2, flash ok/courseId, calls service")
    void postUpdateSelf_admin_ok_redirectsAndSetsFlash() throws Exception {
        mockMvc.perform(post("/courses/self/update")
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("id", COURSE_ID.toString())
                        .param("code", "CS-999")
                        .param("name", "X")
                        .param("teacherId", "5")
                        .param("page", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses?page=2"))
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
                        .param("name", "Some name")
                        .param("page", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("err", "Access denied."));

        verify(courseService, never()).updateSelf(any());
    }

    @Test
    @DisplayName("POST /courses/{id}/delete as ADMIN -> redirects /courses?page=3, flash ok, calls service.delete(id)")
    void postDeleteCourse_deleted_ok() throws Exception {
        doNothing().when(courseService).delete(COURSE_ID);

        mockMvc.perform(post("/courses/{id}/delete", COURSE_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("page", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses?page=3"))
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
                        .with(csrf())
                        .param("page", "3"))
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
                        .with(csrf())
                        .param("page", "1"))
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
                        .param("code", "bad")
                        .param("page", "2"))
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
                        .with(csrf())
                        .param("page", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("err", "Course not found: id=" + COURSE_ID));
    }
}