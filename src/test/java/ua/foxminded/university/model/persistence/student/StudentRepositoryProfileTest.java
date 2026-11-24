package ua.foxminded.university.model.persistence.student;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.model.persistence.student.StudentRepositoryTestConstants.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import ua.foxminded.university.TestcontainersConfiguration;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import(TestcontainersConfiguration.class)
@Sql(
        scripts = "/sql/student/student-repository-base.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class StudentRepositoryProfileTest {

    @Autowired
    private StudentRepository studentRepository;

    @Test
    @DisplayName("findStudentProfileViewById: returns student profile")
    void findStudentProfileViewById_existingStudent_returnsProfile() {
        var result = studentRepository.findStudentProfileViewById(
                STUDENT_1_ID);

        assertTrue(
                result.isPresent());

        var profile = result.orElseThrow();

        assertEquals(
                STUDENT_1_EMAIL,
                profile.email());

        assertEquals(
                STUDENT_1_FIRST_NAME,
                profile.firstName());

        assertEquals(
                STUDENT_1_LAST_NAME,
                profile.lastName());

        assertNotNull(
                profile.createdAt());

        assertEquals(
                STUDENT_1_ENROLLMENT_YEAR,
                profile.enrollmentYear());

        assertEquals(
                GROUP_CS,
                profile.groupName());
    }

    @Test
    @DisplayName("findStudentProfileViewById: missing student -> empty Optional")
    void findStudentProfileViewById_missingStudent_returnsEmpty() {
        var result = studentRepository.findStudentProfileViewById(
                MISSING_ID);

        assertTrue(
                result.isEmpty());
    }

    @Test
    @DisplayName("findGroupIdByStudentId: returns student's group id")
    void findGroupIdByStudentId_existingStudent_returnsGroupId() {
        var result = studentRepository.findGroupIdByStudentId(
                STUDENT_1_ID);

        assertTrue(
                result.isPresent());

        assertEquals(
                GROUP_CS_ID,
                result.orElseThrow());
    }

    @Test
    @DisplayName("findGroupIdByStudentId: soft-deleted student -> empty Optional")
    void findGroupIdByStudentId_softDeletedStudent_returnsEmpty() {
        var result = studentRepository.findGroupIdByStudentId(
                STUDENT_DELETED_ID);

        assertTrue(
                result.isEmpty());
    }

    @Test
    @DisplayName("findGroupIdByStudentId: missing student -> empty Optional")
    void findGroupIdByStudentId_missingStudent_returnsEmpty() {
        var result = studentRepository.findGroupIdByStudentId(
                MISSING_ID);

        assertTrue(
                result.isEmpty());
    }
}