package ua.foxminded.university.web.student;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static ua.foxminded.university.web.student.StudentsManagementWebMvcTestConstants.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ua.foxminded.university.model.persistence.studygroup.projection.GroupView;
import ua.foxminded.university.service.student.StudentService;
import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.student.cards.StudentsCardsPageModel;
import ua.foxminded.university.web.student.cards.StudentsCardsPageModelFactory;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@WebMvcTest(controllers = StudentsManagementController.class)
@Import({
    MethodSecurityTestConfig.class,
    ExceptionMessageReader.class
})
class StudentsManagementControllerWebMvcTest {

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
    @DisplayName("GET /students as authenticated user -> 200, students/students, model has page and group options")
    void getStudents_authenticated_ok()
            throws Exception {

        var page = mock(
                StudentsCardsPageModel.class);

        var groups = List.of(
                new GroupView(
                        GROUP_CS_ID,
                        GROUP_CS),
                new GroupView(
                        GROUP_SE_ID,
                        GROUP_SE));

        when(pageFactory.build(
                any(),
                anyInt()))
                .thenReturn(page);

        when(studyGroupService.listGroupOptions())
                .thenReturn(groups);

        mockMvc.perform(
                get("/students")
                        .param("page", "2")
                        .with(user(
                                Long.toString(ADMIN_ID))
                                .roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name(
                        "students/students"))
                .andExpect(model().attribute(
                        "page",
                        page))
                .andExpect(model().attribute(
                        "all_groups",
                        groups));

        verify(pageFactory)
                .build(
                        any(),
                        org.mockito.ArgumentMatchers.eq(2));

        verify(studyGroupService)
                .listGroupOptions();
    }

    @Test
    @DisplayName("GET /students unauthenticated -> 401, services not called")
    void getStudents_unauthenticated_unauthorized()
            throws Exception {

        mockMvc.perform(
                get("/students"))
                .andExpect(status().isUnauthorized());

        verify(pageFactory, never())
                .build(
                        any(),
                        anyInt());

        verify(studyGroupService, never())
                .listGroupOptions();
    }
}