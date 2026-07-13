package ua.foxminded.university.web.course;

import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ua.foxminded.university.model.persistence.course.projection.CourseHeaderView;
import ua.foxminded.university.model.persistence.studygroup.projection.GroupView;
import ua.foxminded.university.service.course.CourseService;
import ua.foxminded.university.service.course.dto.CourseGroupsView;
import ua.foxminded.university.service.course.exception.CourseGroupsOpException;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@WebMvcTest(controllers = CourseGroupsController.class)
@Import({ CourseGroupsExceptionHandler.class, MethodSecurityTestConfig.class, ExceptionMessageReader.class })
class CourseGroupsControllerWebMvcTest {

    private static final Long USER_ID = 42L;
    private static final Long COURSE_ID = 10L;
    private static final Long GROUP_ID = 35L;

    @Autowired MockMvc mockMvc;
    
    @MockitoBean TeacherService teacherService;
    @MockitoBean CourseService courseService;

    @Test
    @DisplayName("GET /courses/{courseId}/groups as ADMIN -> 200, courses/course-groups, model has page + pageTitle")
    void getPage_admin_ok_returnsViewAndModel() throws Exception {
        var header = new CourseHeaderView(COURSE_ID, "CS-101", "Algorithms");
        var assigned = List.of(new GroupView(1L, "Group A"));
        var available = List.of(new GroupView(2L, "Group B"));
        var page = new CourseGroupsView(header, assigned, available);

        when(courseService.getCourseGroupsView(COURSE_ID)).thenReturn(page);

        mockMvc.perform(get("/courses/{courseId}/groups", COURSE_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/course-groups"))
                .andExpect(model().attribute("pageTitle", "Course groups"))
                .andExpect(model().attribute("page", sameInstance(page)));

        verify(courseService).getCourseGroupsView(COURSE_ID);
    }
    
    @Test
    @DisplayName("GET /courses/{courseId}/groups when EntityNotFoundException -> redirect /courses, flash err")
    void getPage_entityNotFound_redirectsToCoursesAndSetsFlashErr() throws Exception {
        when(courseService.getCourseGroupsView(COURSE_ID))
                .thenThrow(new EntityNotFoundException("Course not found: id=" + COURSE_ID));

        mockMvc.perform(get("/courses/{courseId}/groups", COURSE_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("err", "Course not found: id=" + COURSE_ID));

        verify(courseService).getCourseGroupsView(COURSE_ID);
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
        when(courseService.addGroupToCourse(COURSE_ID, GROUP_ID)).thenReturn(Optional.of(GROUP_ID));

        mockMvc.perform(post("/courses/{courseId}/groups/add", COURSE_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("groupId", GROUP_ID.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + COURSE_ID + "/groups"))
                .andExpect(flash().attribute("ok", "Group added. ID: " + GROUP_ID));

        verify(courseService).addGroupToCourse(COURSE_ID, GROUP_ID);
    }

    @Test
    @DisplayName("POST /courses/{courseId}/groups/add as ADMIN when added=0 -> redirect groups page, flash ok=Nothing to add")
    void postAdd_admin_nothingToAdd_redirectsAndSetsOk() throws Exception {
        when(courseService.addGroupToCourse(COURSE_ID, GROUP_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post("/courses/{courseId}/groups/add", COURSE_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("groupId", GROUP_ID.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + COURSE_ID + "/groups"))
                .andExpect(flash().attribute("ok", "Nothing to add."));

        verify(courseService).addGroupToCourse(COURSE_ID, GROUP_ID);
    }
    
    @Test
    @DisplayName("POST /courses/{courseId}/groups/add when CourseGroupsOpException -> redirect back to groups, flash err")
    void postAdd_courseGroupsOpException_redirectsToGroupsAndSetsErr() throws Exception {
        doThrow(new CourseGroupsOpException(COURSE_ID, "StudyGroup not found: id=" + GROUP_ID))
                .when(courseService).addGroupToCourse(COURSE_ID, GROUP_ID);

        mockMvc.perform(post("/courses/{courseId}/groups/add", COURSE_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("groupId", GROUP_ID.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + COURSE_ID + "/groups"))
                .andExpect(flash().attribute("err", "StudyGroup not found: id=" + GROUP_ID));

        verify(courseService).addGroupToCourse(COURSE_ID, GROUP_ID);
    }
    
    @Test
    @DisplayName("POST /courses/{courseId}/groups/add when DataAccessException -> redirect /courses, flash err")
    void postAdd_dataAccess_redirectsToCoursesAndSetsErr() throws Exception {
        doThrow(new DataAccessException("db down") {})
                .when(courseService).addGroupToCourse(COURSE_ID, GROUP_ID);

        mockMvc.perform(post("/courses/{courseId}/groups/add", COURSE_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("groupId", GROUP_ID.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("err", "db down"));

        verify(courseService).addGroupToCourse(COURSE_ID, GROUP_ID);
    }
    
    @Test
    @DisplayName("POST /courses/{courseId}/groups/add as TEACHER -> 403, service not called")
    void postAdd_teacher_forbidden_serviceNotCalled() throws Exception {
        mockMvc.perform(post("/courses/{courseId}/groups/add", COURSE_ID)
                        .with(user(USER_ID.toString()).roles("TEACHER"))
                        .with(csrf())
                        .param("groupId", "7"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(courseService);
    }

    @Test
    @DisplayName("POST /courses/{courseId}/groups/remove as ADMIN when removed -> redirect back, flash ok=Group removed. ID: X")
    void postRemove_admin_removed_redirectsAndSetsOk() throws Exception {
        when(courseService.removeGroupFromCourse(COURSE_ID, GROUP_ID)).thenReturn(Optional.of(GROUP_ID));

        mockMvc.perform(post("/courses/{courseId}/groups/remove", COURSE_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("groupId", GROUP_ID.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + COURSE_ID + "/groups"))
                .andExpect(flash().attribute("ok", "Group removed. ID: " + GROUP_ID));

        verify(courseService).removeGroupFromCourse(COURSE_ID, GROUP_ID);
    }
    
    @Test
    @DisplayName("POST /courses/{courseId}/groups/remove as ADMIN when nothing to remove -> redirect back, flash ok=Nothing to remove.")
    void postRemove_admin_nothingToRemove_redirectsAndSetsOk() throws Exception {
        when(courseService.removeGroupFromCourse(COURSE_ID, GROUP_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post("/courses/{courseId}/groups/remove", COURSE_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("groupId", GROUP_ID.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/" + COURSE_ID + "/groups"))
                .andExpect(flash().attribute("ok", "Nothing to remove."));

        verify(courseService).removeGroupFromCourse(COURSE_ID, GROUP_ID);
    }

    @Test
    @DisplayName("POST /courses/{courseId}/groups/remove when IllegalArgumentException -> redirect /courses, flash err")
    void postRemove_illegalArgument_redirectsToCoursesAndSetsErr() throws Exception {
        doThrow(new IllegalArgumentException("boom"))
                .when(courseService).removeGroupFromCourse(COURSE_ID, GROUP_ID);

        mockMvc.perform(post("/courses/{courseId}/groups/remove", COURSE_ID)
                        .with(user(USER_ID.toString()).roles("ADMIN"))
                        .with(csrf())
                        .param("groupId", GROUP_ID.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"))
                .andExpect(flash().attribute("err", "boom"));

        verify(courseService).removeGroupFromCourse(COURSE_ID, GROUP_ID);
    }
}
