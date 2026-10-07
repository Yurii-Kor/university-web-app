package ua.foxminded.university.web.group;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ua.foxminded.university.model.persistence.studygroup.projection.GroupCardView;
import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.group.cards.GroupsCardsPageModel;
import ua.foxminded.university.web.group.cards.GroupsCardsPageModelFactory;
import ua.foxminded.university.web.group.cards.strategy.AdminGroupsCardsPageStrategy;
import ua.foxminded.university.web.group.cards.strategy.TeacherGroupsCardsPageStrategy;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;
import ua.foxminded.university.web.util.PrincipalHandler;

@WebMvcTest(controllers = GroupsManagementController.class)
@Import({
    MethodSecurityTestConfig.class,
    GroupsCardsPageModelFactory.class,
    AdminGroupsCardsPageStrategy.class,
    TeacherGroupsCardsPageStrategy.class,
    ExceptionMessageReader.class
})
class GroupsManagementControllerWebMvcTest {

    private static final long ADMIN_ID = 41L;
    private static final long TEACHER_ID = 42L;

    private static final long GROUP_CS_ID = 1001L;
    private static final long GROUP_SE_ID = 1002L;

    private static final String GROUP_CS = "CS-101";
    private static final String GROUP_SE = "SE-205";

    private static final int PAGE_SIZE = 6;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudyGroupService groupService;

    @MockitoBean
    private TeacherService teacherService;

    @MockitoBean
    private PrincipalHandler principalHandler;

    private GroupsCardsPageModel extractPage(
            MvcResult result) {

        var model = result.getModelAndView()
                .getModel();

        var page = model.get("page");

        return (GroupsCardsPageModel) page;
    }

    @Test
    @DisplayName("GET /groups as admin -> 200, groups/groups, model has admin groups page")
    void getGroups_admin_ok_returnsAdminModel()
            throws Exception {

        var firstGroup = new GroupCardView(
                GROUP_CS_ID,
                GROUP_CS,
                20L,
                4L);

        var secondGroup = new GroupCardView(
                GROUP_SE_ID,
                GROUP_SE,
                15L,
                3L);

        var pageable = PageRequest.of(
                0,
                PAGE_SIZE);

        var groupsPage = new PageImpl<>(
                List.of(
                        firstGroup,
                        secondGroup),
                pageable,
                7);

        when(principalHandler.getRole(any()))
                .thenReturn("admin");

        when(principalHandler.parseUserId(any()))
                .thenReturn(ADMIN_ID);

        when(groupService.listGroupCardsForAdmin(
                pageable))
                .thenReturn(groupsPage);

        var result = mockMvc.perform(
                get("/groups")
                        .with(user(
                                Long.toString(ADMIN_ID))
                                .roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name(
                        "groups/groups"))
                .andExpect(model().attributeExists(
                        "page"))
                .andReturn();

        var page = extractPage(result);

        assertEquals(
                "Groups",
                page.pageTitle());

        assertEquals(
                "All study groups in the system.",
                page.pageSubtitle());

        assertIterableEquals(
                List.of(
                        firstGroup,
                        secondGroup),
                page.groups());

        assertTrue(
                page.showAdminActions());

        assertTrue(
                page.showCreateButton());

        assertEquals(
                0,
                page.currentPage());

        assertEquals(
                2,
                page.totalPages());

        assertFalse(
                page.hasPrevious());

        assertTrue(
                page.hasNext());

        verify(groupService)
                .listGroupCardsForAdmin(
                        pageable);
    }

    @Test
    @DisplayName("GET /groups as teacher -> 200, groups/groups, model has teacher groups page")
    void getGroups_teacher_ok_returnsTeacherModel()
            throws Exception {

        var group = new GroupCardView(
                GROUP_CS_ID,
                GROUP_CS,
                20L,
                2L);

        var pageable = PageRequest.of(
                1,
                PAGE_SIZE);

        var groupsPage = new PageImpl<>(
                List.of(group),
                pageable,
                7);

        when(principalHandler.getRole(any()))
                .thenReturn("teacher");

        when(principalHandler.parseUserId(any()))
                .thenReturn(TEACHER_ID);

        when(groupService.listGroupCardsForTeacher(
                TEACHER_ID,
                pageable))
                .thenReturn(groupsPage);

        var result = mockMvc.perform(
                get("/groups")
                        .param("page", "1")
                        .with(user(
                                Long.toString(TEACHER_ID))
                                .roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name(
                        "groups/groups"))
                .andExpect(model().attributeExists(
                        "page"))
                .andReturn();

        var page = extractPage(result);

        assertEquals(
                "My groups",
                page.pageTitle());

        assertEquals(
                "Groups where you teach.",
                page.pageSubtitle());

        assertIterableEquals(
                List.of(group),
                page.groups());

        assertFalse(
                page.showAdminActions());

        assertFalse(
                page.showCreateButton());

        assertEquals(
                1,
                page.currentPage());

        assertEquals(
                2,
                page.totalPages());

        assertTrue(
                page.hasPrevious());

        assertFalse(
                page.hasNext());

        verify(groupService)
                .listGroupCardsForTeacher(
                        TEACHER_ID,
                        pageable);
    }

    @Test
    @DisplayName("GET /groups as student -> 403, services not called")
    void getGroups_student_forbidden()
            throws Exception {

        mockMvc.perform(
                get("/groups")
                        .with(user("43")
                                .roles("STUDENT")))
                .andExpect(status().isForbidden());

        verify(groupService, never())
                .listGroupCardsForAdmin(
                        any());

        verify(groupService, never())
                .listGroupCardsForTeacher(
                        anyLong(),
                        any());
    }

    @Test
    @DisplayName("GET /groups unauthenticated -> 401, services not called")
    void getGroups_unauthenticated_unauthorized()
            throws Exception {

        mockMvc.perform(
                get("/groups"))
                .andExpect(status().isUnauthorized());

        verify(groupService, never())
                .listGroupCardsForAdmin(
                        any());

        verify(groupService, never())
                .listGroupCardsForTeacher(
                        anyLong(),
                        any());
    }
}