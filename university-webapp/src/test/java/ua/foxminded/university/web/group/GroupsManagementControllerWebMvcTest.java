package ua.foxminded.university.web.group;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

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

import ua.foxminded.university.model.repository.dto.GroupView;
import ua.foxminded.university.service.StudyGroupService;
import ua.foxminded.university.service.TeacherService;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@WebMvcTest(controllers = GroupsManagementController.class)
@Import({
    MethodSecurityTestConfig.class,
    ExceptionMessageReader.class
})
class GroupsManagementControllerWebMvcTest {

    @Autowired MockMvc mockMvc;
    
    @MockitoBean TeacherService teacherService;

    @MockitoBean StudyGroupService studyGroupService;

    @Test
    @DisplayName("GET /groups -> 200, groups/groups, model has paged groups")
    void getGroups_ok_returnsViewAndModel() throws Exception {
        var firstGroup = new GroupView(1L, "AA-11");
        var secondGroup = new GroupView(2L, "BB-22");

        var groupsPage = new PageImpl<>(
                List.of(firstGroup, secondGroup),
                PageRequest.of(0, 10),
                11
        );

        when(studyGroupService.listGroupCardsForAdmin(any(Pageable.class)))
                .thenReturn(groupsPage);

        mockMvc.perform(get("/groups")
                        .with(user("42").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("groups/groups"))
                .andExpect(model().attribute("pageTitle", "Groups"))
                .andExpect(model().attribute("groups", contains(firstGroup, secondGroup)))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 2))
                .andExpect(model().attribute("hasPrevious", false))
                .andExpect(model().attribute("hasNext", true));

        verify(studyGroupService).listGroupCardsForAdmin(argThat(p -> p.getPageNumber() == 0));
    }
}