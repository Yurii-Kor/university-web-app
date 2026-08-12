package ua.foxminded.university.web.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ua.foxminded.university.model.persistence.studygroup.projection.StudentGroupHeaderView;
import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.group.details.GroupDetailsPageModelFactory;
import ua.foxminded.university.web.group.details.StudentGroupPageModel;
import ua.foxminded.university.web.group.details.strategy.StudentGroupDetailsPageStrategy;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;
import ua.foxminded.university.web.util.PrincipalHandler;

@WebMvcTest(controllers = GroupDetailsController.class)
@Import({
    MethodSecurityTestConfig.class,
    GroupDetailsPageModelFactory.class,
    StudentGroupDetailsPageStrategy.class,
    ExceptionMessageReader.class
})
class GroupDetailsControllerWebMvcTest {

    private static final long STUDENT_ID = 42L;
    private static final long GROUP_ID = 1001L;

    private static final String GROUP_NAME = "CS-101";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudyGroupService groupService;

    @MockitoBean
    private TeacherService teacherService;

    @MockitoBean
    private PrincipalHandler principalHandler;

    private StudentGroupPageModel extractPage(
            MvcResult result) {

        var model = result.getModelAndView()
                .getModel();

        var page = model.get("page");

        return assertInstanceOf(
                StudentGroupPageModel.class,
                page);
    }

    @Test
    @DisplayName("GET /group as student -> 200, student group view, model has group details")
    void getGroup_student_ok_returnsStudentGroupModel()
            throws Exception {

        var group = new StudentGroupHeaderView(
                GROUP_ID,
                GROUP_NAME,
                20L);

        when(principalHandler.getRole(any()))
                .thenReturn("student");

        when(principalHandler.parseUserId(any()))
                .thenReturn(STUDENT_ID);

        when(groupService.getGroupHeaderForStudent(
                STUDENT_ID))
                .thenReturn(group);

        var result = mockMvc.perform(
                get("/group")
                        .with(user(
                                Long.toString(STUDENT_ID))
                                .roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name(
                        "groups/details/student"))
                .andExpect(model().attributeExists(
                        "page"))
                .andReturn();

        var page = extractPage(result);

        assertEquals(
                "My group",
                page.pageTitle());

        assertEquals(
                "Your study group.",
                page.pageSubtitle());

        assertEquals(
                GROUP_ID,
                page.groupId());

        assertEquals(
                GROUP_NAME,
                page.groupName());

        assertEquals(
                20L,
                page.studentCount());

        verify(groupService)
                .getGroupHeaderForStudent(
                        STUDENT_ID);
    }

    @Test
    @DisplayName("GET /group as admin -> 403, service not called")
    void getGroup_admin_forbidden()
            throws Exception {

        mockMvc.perform(
                get("/group")
                        .with(user("41")
                                .roles("ADMIN")))
                .andExpect(status().isForbidden());

        verify(groupService, never())
                .getGroupHeaderForStudent(
                        anyLong());
    }

    @Test
    @DisplayName("GET /group as teacher -> 403, service not called")
    void getGroup_teacher_forbidden()
            throws Exception {

        mockMvc.perform(
                get("/group")
                        .with(user("43")
                                .roles("TEACHER")))
                .andExpect(status().isForbidden());

        verify(groupService, never())
                .getGroupHeaderForStudent(
                        anyLong());
    }

    @Test
    @DisplayName("GET /group unauthenticated -> 401, service not called")
    void getGroup_unauthenticated_unauthorized()
            throws Exception {

        mockMvc.perform(
                get("/group"))
                .andExpect(status().isUnauthorized());

        verify(groupService, never())
                .getGroupHeaderForStudent(
                        anyLong());
    }
}