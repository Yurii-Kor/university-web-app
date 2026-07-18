package ua.foxminded.university.web.teacher;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.OffsetDateTime;
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

import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.persistence.teacher.projection.TeacherCardView;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@WebMvcTest(controllers = TeachersManagementController.class)
@Import({
    MethodSecurityTestConfig.class,
    ExceptionMessageReader.class
})
class TeachersManagementControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    TeacherService teacherService;

    @Test
    @DisplayName("GET /teachers -> 200, teachers/teachers, model has paged teachers")
    void getTeachers_ok_returnsViewAndModel() throws Exception {
        var now = OffsetDateTime.parse("2025-01-01T12:00:00Z");
        var someRank = AcademicRank.values()[0];

        var firstTeacher = new TeacherCardView(
                1L,
                "john.teacher@university.com",
                "John",
                "Smith",
                true,
                now,
                someRank,
                "B-214"
        );

        var secondTeacher = new TeacherCardView(
                2L,
                "kate.teacher@university.com",
                "Kate",
                "Brown",
                false,
                now.minusDays(1),
                someRank,
                "C-105"
        );

        var teachersPage = new PageImpl<>(
                List.of(firstTeacher, secondTeacher),
                PageRequest.of(0, 10),
                11
        );

        when(teacherService.listTeacherCardsForAdmin(any(Pageable.class)))
                .thenReturn(teachersPage);

        mockMvc.perform(get("/teachers")
                        .with(user("42").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("teachers/teachers"))
                .andExpect(model().attribute("pageTitle", "Teachers"))
                .andExpect(model().attribute("teachers", contains(
                        firstTeacher,
                        secondTeacher
                )))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 2))
                .andExpect(model().attribute("hasPrevious", false))
                .andExpect(model().attribute("hasNext", true));

        verify(teacherService).listTeacherCardsForAdmin(argThat(p -> p.getPageNumber() == 0));
    }
}