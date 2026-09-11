package ua.foxminded.university.service.lesson;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ua.foxminded.university.service.lesson.LessonServiceTestConstants.*;

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
@Import({
        TestcontainersConfiguration.class,
        LessonServiceTestConfig.class
})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
        scripts = "/sql/lesson/lesson-service-base.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
        scripts = "/sql/cleanup-database.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class LessonServiceFindTest {

    @Autowired
    private LessonService lessonService;

    @Test
    @DisplayName("listGroupSchedule: full day -> returns group lessons ordered by start time")
    void listGroupSchedule_fullDay_returnsOrderedLessons() {
        var lessons = lessonService.listGroupSchedule(
                GROUP_1_ID,
                SCHEDULE_DAY_START,
                SCHEDULE_DAY_END);

        assertAll(
                () -> assertEquals(
                        2,
                        lessons.size()),
                () -> assertEquals(
                        STARTED_LESSON_ID,
                        lessons.get(0)
                                .getId()),
                () -> assertEquals(
                        GROUP_CONFLICT_LESSON_ID,
                        lessons.get(1)
                                .getId()),
                () -> assertEquals(
                        GROUP_1_ID,
                        lessons.get(0)
                                .getGroup()
                                .getId()),
                () -> assertEquals(
                        GROUP_1_ID,
                        lessons.get(1)
                                .getGroup()
                                .getId()));
    }

    @Test
    @DisplayName("listGroupSchedule: range is [from, to) -> includes from and excludes to")
    void listGroupSchedule_halfOpenRange_respectsBoundaries() {
        var lessons = lessonService.listGroupSchedule(
                GROUP_1_ID,
                GROUP_RANGE_START,
                GROUP_RANGE_END);

        assertAll(
                () -> assertEquals(
                        1,
                        lessons.size()),
                () -> assertEquals(
                        STARTED_LESSON_ID,
                        lessons.getFirst()
                                .getId()),
                () -> assertEquals(
                        GROUP_RANGE_START,
                        lessons.getFirst()
                                .getStartTime()));
    }
    
    @Test
    @DisplayName("listTeacherSchedule: full day -> returns teacher lessons ordered by start time")
    void listTeacherSchedule_fullDay_returnsOrderedLessons() {
        var lessons = lessonService.listTeacherSchedule(
                TEACHER_1_ID,
                SCHEDULE_DAY_START,
                SCHEDULE_DAY_END);

        assertAll(
                () -> assertEquals(
                        2,
                        lessons.size()),
                () -> assertEquals(
                        STARTED_LESSON_ID,
                        lessons.get(0)
                                .getId()),
                () -> assertEquals(
                        STARTED_LESSON_START,
                        lessons.get(0)
                                .getStartTime()),
                () -> assertEquals(
                        TEACHER_CONFLICT_LESSON_ID,
                        lessons.get(1)
                                .getId()),
                () -> assertEquals(
                        TEACHER_CONFLICT_LESSON_START,
                        lessons.get(1)
                                .getStartTime()));
    }

    @Test
    @DisplayName("listTeacherSchedule: range is [from, to) -> includes from and excludes to")
    void listTeacherSchedule_halfOpenRange_respectsBoundaries() {
        var lessons = lessonService.listTeacherSchedule(
                TEACHER_2_ID,
                TEACHER_RANGE_START,
                TEACHER_RANGE_END);

        assertAll(
                () -> assertEquals(
                        1,
                        lessons.size()),
                () -> assertEquals(
                        GROUP_CONFLICT_LESSON_ID,
                        lessons.getFirst()
                                .getId()),
                () -> assertEquals(
                        TEACHER_RANGE_START,
                        lessons.getFirst()
                                .getStartTime()));
    }

    @Test
    @DisplayName("listGroupSchedule: no lessons in range -> returns empty list")
    void listGroupSchedule_noLessonsInRange_returnsEmptyList() {
        var lessons = lessonService.listGroupSchedule(
                GROUP_1_ID,
                EMPTY_RANGE_START,
                EMPTY_RANGE_END);

        assertTrue(
                lessons.isEmpty());
    }

    @Test
    @DisplayName("listTeacherSchedule: no lessons in range -> returns empty list")
    void listTeacherSchedule_noLessonsInRange_returnsEmptyList() {
        var lessons = lessonService.listTeacherSchedule(
                TEACHER_1_ID,
                EMPTY_RANGE_START,
                EMPTY_RANGE_END);

        assertTrue(
                lessons.isEmpty());
    }
    
    @Test
    @DisplayName("listTeacherSchedule: uses assigned teacher instead of current course teacher")
    void listTeacherSchedule_usesAssignedTeacher() {
        var teacher2Lessons = lessonService.listTeacherSchedule(
                TEACHER_2_ID,
                ASSIGNED_TEACHER_RANGE_START,
                ASSIGNED_TEACHER_RANGE_END);

        var teacher1Lessons = lessonService.listTeacherSchedule(
                TEACHER_1_ID,
                ASSIGNED_TEACHER_RANGE_START,
                ASSIGNED_TEACHER_RANGE_END);

        assertAll(
                () -> assertEquals(
                        1,
                        teacher2Lessons.size()),
                () -> assertEquals(
                        ASSIGNED_TEACHER_2_LESSON_ID,
                        teacher2Lessons.getFirst()
                                .getId()),
                () -> assertTrue(
                        teacher1Lessons.isEmpty()));
    }
}