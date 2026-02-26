package ua.foxminded.university.web.course;

import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ua.foxminded.university.model.repository.dto.CourseHeaderView;
import ua.foxminded.university.model.repository.dto.GroupOptionView;
import ua.foxminded.university.service.CourseService;
import ua.foxminded.university.service.TeacherService;
import ua.foxminded.university.service.dto.response.CourseGroupsPageView;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;

@WebMvcTest(controllers = CourseGroupsController.class)
@Import({ CourseGroupsExceptionHandler.class, MethodSecurityTestConfig.class })
class CourseGroupsControllerWebMvcTest {

    private static final Long USER_ID = 42L;
    private static final Long COURSE_ID = 10L;

    @Autowired
    MockMvc mockMvc;
    
    @MockitoBean
    TeacherService teacherService;

    @MockitoBean
    CourseService courseService;

    @Test
    @DisplayName("GET /courses/{courseId}/groups as ADMIN -> 200, courses/course-groups, model has page + pageTitle")
    void getPage_admin_ok_returnsViewAndModel() throws Exception {
        long courseId = 10L;

        var header = new CourseHeaderView(courseId, "CS-101", "Algorithms");
        var assigned = List.of(new GroupOptionView(1L, "Group A"));
        var available = List.of(new GroupOptionView(2L, "Group B"));
        var page = new CourseGroupsPageView(header, assigned, available);

        when(courseService.getCourseGroupsPage(courseId)).thenReturn(page);

        mockMvc.perform(get("/courses/{courseId}/groups", courseId)
                        .with(user(USER_ID.toString()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/course-groups"))
                .andExpect(model().attribute("pageTitle", "Course groups"))
                .andExpect(model().attribute("page", sameInstance(page)));

        verify(courseService).getCourseGroupsPage(courseId);
    }

    @Test
    @DisplayName("GET /courses/{courseId}/groups as TEACHER -> 403, service not called")
    void getPage_teacher_forbidden_is403() throws Exception {
        mockMvc.perform(get("/courses/{courseId}/groups", COURSE_ID)
                        .with(user(USER_ID.toString()).roles("TEACHER")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    @Test
    @DisplayName("GET /courses/{courseId}/groups as STUDENT -> 403, service not called")
    void getPage_student_forbidden_is403() throws Exception {
        mockMvc.perform(get("/courses/{courseId}/groups", COURSE_ID)
                        .with(user(USER_ID.toString()).roles("STUDENT")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    @Test
    @DisplayName("POST /courses/{courseId}/groups/add as ADMIN when added>0 -> redirect groups page, flash ok=Group added")
    void postAdd_admin_added_redirectsAndSetsOk() throws Exception {
        long courseId = 10L;
        long groupId = 7L;

        when(courseService.addGroupsToCourse(courseId, List.of(groupId))).thenReturn(1);

        mockMvc.perform(post("/courses/{courseId}/groups/add", courseId)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("groupId", String.valueOf(groupId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + courseId + "/groups"))
                .andExpect(flash().attribute("ok", "Group added."));

        verify(courseService).addGroupsToCourse(courseId, List.of(groupId));
    }

    @Test
    @DisplayName("POST /courses/{courseId}/groups/add as ADMIN when added=0 -> redirect groups page, flash ok=Nothing to add")
    void postAdd_admin_nothingToAdd_redirectsAndSetsOk() throws Exception {
        long courseId = 10L;
        long groupId = 7L;

        when(courseService.addGroupsToCourse(courseId, List.of(groupId))).thenReturn(0);

        mockMvc.perform(post("/courses/{courseId}/groups/add", courseId)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("groupId", String.valueOf(groupId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + courseId + "/groups"))
                .andExpect(flash().attribute("ok", "Nothing to add."));

        verify(courseService).addGroupsToCourse(courseId, List.of(groupId));
    }

    @Test
    @DisplayName("POST /courses/{courseId}/groups/remove as ADMIN when removed>0 -> redirect groups page, flash ok=Group removed")
    void postRemove_admin_removed_redirectsAndSetsOk() throws Exception {
        long courseId = 10L;
        long groupId = 7L;

        when(courseService.removeGroupsFromCourse(courseId, List.of(groupId))).thenReturn(1);

        mockMvc.perform(post("/courses/{courseId}/groups/remove", courseId)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("groupId", String.valueOf(groupId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + courseId + "/groups"))
                .andExpect(flash().attribute("ok", "Group removed."));

        verify(courseService).removeGroupsFromCourse(courseId, List.of(groupId));
    }

    @Test
    @DisplayName("GET /courses/{courseId}/groups when service throws EntityNotFoundException -> redirects /courses, flash err + courseId")
    void getPage_entityNotFound_redirectsToCoursesAndSetsFlash() throws Exception {
        long courseId = 999L;

        when(courseService.getCourseGroupsPage(courseId))
                .thenThrow(new EntityNotFoundException("Course not found: id=" + courseId));

        mockMvc.perform(get("/courses/{courseId}/groups", courseId)
                        .with(user(USER_ID.toString()).roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("err", "Course not found: id=" + courseId))
                .andExpect(flash().attribute("courseId", courseId));

        verify(courseService).getCourseGroupsPage(courseId);
    }

    @Test
    @DisplayName("POST /courses/{courseId}/groups/add when service throws IllegalArgumentException -> redirect groups page, flash err (advice)")
    void postAdd_illegalArgument_redirectsWithFlashErrFromAdvice() throws Exception {
        long courseId = 10L;
        long groupId = 7L;

        when(courseService.addGroupsToCourse(courseId, List.of(groupId)))
                .thenThrow(new IllegalArgumentException("StudyGroups not found: [7]"));

        mockMvc.perform(post("/courses/{courseId}/groups/add", courseId)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("groupId", String.valueOf(groupId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + courseId + "/groups"))
                .andExpect(flash().attribute("err", "StudyGroups not found: [7]"));
    }

    @Test
    @DisplayName("POST /courses/{courseId}/groups/remove when DataAccessException -> redirect groups page, flash err=message (advice known)")
    void postRemove_dataAccess_redirectsWithFlashErrFromAdvice() throws Exception {
        long courseId = 10L;
        long groupId = 7L;

        when(courseService.removeGroupsFromCourse(courseId, List.of(groupId)))
                .thenThrow(new DataIntegrityViolationException("db conflict"));

        mockMvc.perform(post("/courses/{courseId}/groups/remove", courseId)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("groupId", String.valueOf(groupId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + courseId + "/groups"))
                .andExpect(flash().attribute("err", "db conflict"));
    }
}
