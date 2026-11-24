package ua.foxminded.university.service.teacher;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.teacher.TeacherServiceTestConstants.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import jakarta.persistence.EntityNotFoundException;
import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.persistence.teacher.TeacherRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
        scripts = "/sql/teacher/teacher-service-base.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "/sql/cleanup-database.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class TeacherServiceDeleteTest {

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private TeacherRepository teacherRepository;

    @Test
    @DisplayName("deleteById: teacher without courses -> soft deletes teacher")
    void deleteById_teacherWithoutCourses_success() {
        teacherService.deleteById(
                TEACHER_3_ID);

        assertTrue(
                teacherRepository.findById(
                        TEACHER_3_ID)
                        .isEmpty(),
                "soft-deleted teacher must not be visible through normal repository queries");

        assertThrows(
                EntityNotFoundException.class,
                () -> teacherService.getTeacherProfileView(
                        TEACHER_3_ID),
                "soft-deleted teacher must no longer be visible");
    }

    @Test
    @DisplayName("deleteById: teacher with courses -> IllegalStateException")
    void deleteById_teacherWithCourses_fails() {
        var exception = assertThrows(
                IllegalStateException.class,
                () -> teacherService.deleteById(
                        TEACHER_1_ID));

        assertEquals(
                "Cannot delete teacher with assigned courses: id="
                        + TEACHER_1_ID
                        + ", coursesCount=2",
                exception.getMessage());

        assertTrue(
                teacherRepository.findById(
                        TEACHER_1_ID)
                        .isPresent(),
                "teacher must remain active after failed delete");
    }

    @Test
    @DisplayName("deleteById: missing teacher -> EntityNotFoundException")
    void deleteById_missingTeacher_fails() {
        var exception = assertThrows(
                EntityNotFoundException.class,
                () -> teacherService.deleteById(
                        MISSING_ID));

        assertEquals(
                "Teacher not found: id="
                        + MISSING_ID,
                exception.getMessage());
    }
}