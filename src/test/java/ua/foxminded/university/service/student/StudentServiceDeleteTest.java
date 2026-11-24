package ua.foxminded.university.service.student;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.student.StudentServiceTestConstants.*;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import ua.foxminded.university.TestcontainersConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
        scripts = "/sql/student/student-service-base.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "/sql/cleanup-database.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class StudentServiceDeleteTest {

    @Autowired
    private StudentService studentService;

    @Test
    @DisplayName("deleteById: existing student -> soft deletes student")
    void deleteById_existingStudent_success() {
        studentService.deleteById(
                STUDENT_1_ID);

        assertThrows(
                EntityNotFoundException.class,
                () -> studentService.getStudentProfileView(
                        STUDENT_1_ID),
                "soft-deleted student must no longer be visible");
    }

    @Test
    @DisplayName("deleteById: missing student -> EntityNotFoundException")
    void deleteById_missingStudent_fails() {
        var exception = assertThrows(
                EntityNotFoundException.class,
                () -> studentService.deleteById(
                        MISSING_ID));

        assertEquals(
                "Student not found: id="
                        + MISSING_ID,
                exception.getMessage());
    }
}