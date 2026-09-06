package ua.foxminded.university.model.persistence.teacher;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.model.persistence.teacher.TeacherRepositoryTestConstants.*;

import java.util.List;

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
        scripts = "/sql/teacher/teacher-repository-base.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class TeacherRepositoryProfileTest {

    @Autowired
    private TeacherRepository teacherRepository;

    @Test
    @DisplayName("findTeacherProfileViewById: returns teacher profile")
    void findTeacherProfileViewById_existingTeacher_returnsProfile() {
        var result = teacherRepository.findTeacherProfileViewById(
                TEACHER_1_ID);

        assertTrue(
                result.isPresent());

        var profile = result.orElseThrow();

        assertAll(
                () -> assertEquals(
                        TEACHER_1_EMAIL,
                        profile.email()),
                () -> assertEquals(
                        TEACHER_1_FIRST_NAME,
                        profile.firstName()),
                () -> assertEquals(
                        TEACHER_1_LAST_NAME,
                        profile.lastName()),
                () -> assertNotNull(
                        profile.createdAt()),
                () -> assertEquals(
                        TEACHER_1_RANK,
                        profile.academicRank()),
                () -> assertEquals(
                        TEACHER_1_OFFICE,
                        profile.office()));
    }

    @Test
    @DisplayName("findTeacherProfileViewById: soft-deleted teacher -> empty Optional")
    void findTeacherProfileViewById_softDeletedTeacher_returnsEmpty() {
        var result = teacherRepository.findTeacherProfileViewById(
                TEACHER_DELETED_ID);

        assertTrue(
                result.isEmpty());
    }

    @Test
    @DisplayName("findTeacherProfileViewById: missing teacher -> empty Optional")
    void findTeacherProfileViewById_missingTeacher_returnsEmpty() {
        var result = teacherRepository.findTeacherProfileViewById(
                MISSING_ID);

        assertTrue(
                result.isEmpty());
    }

    @Test
    @DisplayName("findTeacherOptions: returns active teachers in display order")
    void findTeacherOptions_returnsActiveTeachers() {
        var result = teacherRepository.findTeacherOptions();

        assertEquals(
                ACTIVE_TEACHER_COUNT,
                result.size());

        assertEquals(
                List.of(
                        TEACHER_2_ID,
                        TEACHER_3_ID,
                        TEACHER_1_ID,
                        TEACHER_BLOCKED_ID),
                result.stream()
                        .map(option -> option.id())
                        .toList());

        assertFalse(
                result.stream()
                        .anyMatch(option ->
                                option.id().equals(
                                        TEACHER_DELETED_ID)));

        var teacher = result.stream()
                .filter(option ->
                        option.id().equals(
                                TEACHER_1_ID))
                .findFirst()
                .orElseThrow();

        assertAll(
                () -> assertEquals(
                        TEACHER_1_FIRST_NAME,
                        teacher.firstName()),
                () -> assertEquals(
                        TEACHER_1_LAST_NAME,
                        teacher.lastName()),
                () -> assertEquals(
                        TEACHER_1_EMAIL,
                        teacher.email()));
    }
}