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
import ua.foxminded.university.model.persistence.student.StudentRepository;
import ua.foxminded.university.service.student.dto.StudentUpdateDto;
import ua.foxminded.university.service.student.exception.StudentUpdateException;

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
class StudentServiceUpdateTest {

    @Autowired
    private StudentService studentService;

    @Autowired
    private StudentRepository studentRepository;

    @Test
    @DisplayName("update: happy path — updates group and enrollment year")
    void update_happyPath_updatesGroupAndEnrollmentYear() {
        var dto = new StudentUpdateDto(
                GROUP_ME_ID,
                UPDATED_ENROLLMENT_YEAR);

        studentService.update(
                STUDENT_1_ID,
                dto);

        var student = studentRepository.findById(
                STUDENT_1_ID)
                .orElseThrow();

        assertAll(
                () -> assertEquals(
                        GROUP_ME_ID,
                        student.getGroup().getId()),
                () -> assertEquals(
                        UPDATED_ENROLLMENT_YEAR,
                        student.getEnrollmentYear()));
    }

    @Test
    @DisplayName("update: group changed — updates group only")
    void update_groupChanged_updatesGroupOnly() {
        var studentBefore = studentRepository.findById(
                STUDENT_1_ID)
                .orElseThrow();

        var enrollmentYear = studentBefore.getEnrollmentYear();

        var dto = new StudentUpdateDto(
                GROUP_ME_ID,
                enrollmentYear);

        studentService.update(
                STUDENT_1_ID,
                dto);

        var studentAfter = studentRepository.findById(
                STUDENT_1_ID)
                .orElseThrow();

        assertAll(
                () -> assertEquals(
                        GROUP_ME_ID,
                        studentAfter.getGroup().getId()),
                () -> assertEquals(
                        enrollmentYear,
                        studentAfter.getEnrollmentYear()));
    }

    @Test
    @DisplayName("update: enrollment year changed — updates enrollment year only")
    void update_enrollmentYearChanged_updatesEnrollmentYearOnly() {
        var dto = new StudentUpdateDto(
                GROUP_CS_ID,
                UPDATED_ENROLLMENT_YEAR);

        studentService.update(
                STUDENT_1_ID,
                dto);

        var student = studentRepository.findById(
                STUDENT_1_ID)
                .orElseThrow();

        assertAll(
                () -> assertEquals(
                        GROUP_CS_ID,
                        student.getGroup().getId()),
                () -> assertEquals(
                        UPDATED_ENROLLMENT_YEAR,
                        student.getEnrollmentYear()));
    }

    @Test
    @DisplayName("update: missing student -> EntityNotFoundException")
    void update_missingStudent_fails() {
        var dto = new StudentUpdateDto(
                GROUP_ME_ID,
                UPDATED_ENROLLMENT_YEAR);

        var exception = assertThrows(
                EntityNotFoundException.class,
                () -> studentService.update(
                        MISSING_ID,
                        dto));

        assertEquals(
                "Student not found: id="
                        + MISSING_ID,
                exception.getMessage());
    }

    @Test
    @DisplayName("update: missing target group -> StudentUpdateException")
    void update_missingGroup_fails() {
        var studentBefore = studentRepository.findById(
                STUDENT_1_ID)
                .orElseThrow();

        var enrollmentYearBefore = studentBefore.getEnrollmentYear();

        var dto = new StudentUpdateDto(
                MISSING_ID,
                UPDATED_ENROLLMENT_YEAR);

        var exception = assertThrows(
                StudentUpdateException.class,
                () -> studentService.update(
                        STUDENT_1_ID,
                        dto));

        assertEquals(
                STUDENT_1_ID,
                exception.getStudentId());

        assertEquals(
                "StudyGroup not found: id="
                        + MISSING_ID,
                exception.getMessage());

        var studentAfter = studentRepository.findById(
                STUDENT_1_ID)
                .orElseThrow();

        assertAll(
                () -> assertEquals(
                        GROUP_CS_ID,
                        studentAfter.getGroup().getId(),
                        "student group must remain unchanged after failed update"),
                () -> assertEquals(
                        enrollmentYearBefore,
                        studentAfter.getEnrollmentYear(),
                        "student enrollment year must remain unchanged after failed update"));
    }
}