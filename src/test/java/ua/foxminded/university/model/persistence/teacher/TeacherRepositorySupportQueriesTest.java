package ua.foxminded.university.model.persistence.teacher;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.model.persistence.teacher.TeacherRepositoryTestConstants.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.persistence.appuser.AppUserRepository;

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
class TeacherRepositorySupportQueriesTest {

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    @DisplayName("countCoursesByTeacherId: returns assigned course count")
    void countCoursesByTeacherId_teacherWithCourses_returnsCount() {
        var count = teacherRepository.countCoursesByTeacherId(
                TEACHER_1_ID);

        assertEquals(
                TEACHER_1_COURSES_COUNT,
                count);
    }

    @Test
    @DisplayName("countCoursesByTeacherId: teacher without courses returns zero")
    void countCoursesByTeacherId_teacherWithoutCourses_returnsZero() {
        var count = teacherRepository.countCoursesByTeacherId(
                TEACHER_3_ID);

        assertEquals(
                0L,
                count);
    }

    @Test
    @DisplayName("findExistingIds: returns only active existing teacher ids")
    void findExistingIds_mixedIds_returnsExistingActiveIds() {
        var result = teacherRepository.findExistingIds(
                List.of(
                        TEACHER_1_ID,
                        TEACHER_3_ID,
                        TEACHER_DELETED_ID,
                        MISSING_ID));

        var ids = result.stream()
                .collect(Collectors.toSet());

        assertEquals(
                Set.of(
                        TEACHER_1_ID,
                        TEACHER_3_ID),
                ids);
    }

    @Test
    @DisplayName("deleteProfileById: deletes teacher profile but keeps app user")
    void deleteProfileById_existingTeacher_deletesProfileOnly() {
        var deleted = teacherRepository.deleteProfileById(
                TEACHER_3_ID);

        assertEquals(
                1,
                deleted);

        assertTrue(
                teacherRepository.findById(
                        TEACHER_3_ID)
                        .isEmpty());

        assertTrue(
                appUserRepository.findById(
                        TEACHER_3_ID)
                        .isPresent(),
                "role-change profile deletion must keep the app user");
    }

    @Test
    @DisplayName("deleteProfileById: missing teacher returns zero")
    void deleteProfileById_missingTeacher_returnsZero() {
        var deleted = teacherRepository.deleteProfileById(
                MISSING_ID);

        assertEquals(
                0,
                deleted);
    }

    @Test
    @DisplayName("findTeacherWithCoursesCountById: returns teacher and course count")
    void findTeacherWithCoursesCountById_teacherWithCourses_returnsProjection() {
        var result = teacherRepository.findTeacherWithCoursesCountById(
                TEACHER_1_ID);

        assertTrue(
                result.isPresent());

        var projection = result.orElseThrow();

        assertAll(
                () -> assertEquals(
                        TEACHER_1_ID,
                        projection.teacher()
                                .getId()),
                () -> assertEquals(
                        TEACHER_1_COURSES_COUNT,
                        projection.coursesCount()));
    }

    @Test
    @DisplayName("findTeacherWithCoursesCountById: teacher without courses returns zero")
    void findTeacherWithCoursesCountById_teacherWithoutCourses_returnsZero() {
        var result = teacherRepository.findTeacherWithCoursesCountById(
                TEACHER_3_ID);

        assertTrue(
                result.isPresent());

        assertEquals(
                0L,
                result.orElseThrow()
                        .coursesCount());
    }

    @Test
    @DisplayName("findTeacherWithCoursesCountById: missing teacher -> empty Optional")
    void findTeacherWithCoursesCountById_missingTeacher_returnsEmpty() {
        var result = teacherRepository.findTeacherWithCoursesCountById(
                MISSING_ID);

        assertTrue(
                result.isEmpty());
    }
}