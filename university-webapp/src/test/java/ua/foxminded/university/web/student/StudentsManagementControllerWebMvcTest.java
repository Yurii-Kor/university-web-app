package ua.foxminded.university.web.student;

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

import ua.foxminded.university.model.repository.dto.StudentCardView;
import ua.foxminded.university.service.StudentService;
import ua.foxminded.university.service.TeacherService;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@WebMvcTest(controllers = StudentsManagementController.class)
@Import({
    MethodSecurityTestConfig.class,
    ExceptionMessageReader.class
})
class StudentsManagementControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    StudentService studentService;

    @MockitoBean
    TeacherService teacherService;

    @Test
    @DisplayName("GET /students -> 200, students/students, model has paged students")
    void getStudents_ok_returnsViewAndModel() throws Exception {
        var now = OffsetDateTime.parse("2025-01-01T12:00:00Z");

        var firstStudent = new StudentCardView(
                1L,
                "john@student.com",
                "John",
                "Smith",
                true,
                now,
                2024,
                "AA-11"
        );

        var secondStudent = new StudentCardView(
                2L,
                "kate@student.com",
                "Kate",
                "Brown",
                false,
                now.minusDays(1),
                2023,
                "BB-22"
        );

        var studentsPage = new PageImpl<>(
                List.of(firstStudent, secondStudent),
                PageRequest.of(0, 10),
                11
        );

        when(studentService.listStudentCardsForAdmin(any(Pageable.class)))
                .thenReturn(studentsPage);

        mockMvc.perform(get("/students")
                        .with(user("42").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("students/students"))
                .andExpect(model().attribute("pageTitle", "Students"))
                .andExpect(model().attribute("students", contains(
                        firstStudent,
                        secondStudent
                )))
                .andExpect(model().attribute("currentPage", 0))
                .andExpect(model().attribute("totalPages", 2))
                .andExpect(model().attribute("hasPrevious", false))
                .andExpect(model().attribute("hasNext", true));

        verify(studentService).listStudentCardsForAdmin(argThat(p -> p.getPageNumber() == 0));
    }
}