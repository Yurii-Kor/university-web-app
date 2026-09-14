package ua.foxminded.university.model.persistence.lesson;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.model.persistence.lesson.LessonRepositoryTestConstants.*;

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
        scripts = "/sql/lesson/lesson-repository-base.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class LessonRepositoryScheduleTest {

    @Autowired
    private LessonRepository lessonRepository;

    @Test
    @DisplayName("findGroupSchedule: returns group lessons inside half-open time window ordered by start")
    void findGroupSchedule_returnsLessonsInsideWindowOrdered() {

        var lessons =
                lessonRepository.findGroupSchedule(
                        GROUP_1_ID,
                        SCHEDULE_FROM,
                        SCHEDULE_TO);

        assertEquals(
                List.of(
                        LESSON_BASE_ID,
                        LESSON_GROUP_OVERLAP_ID),
                lessons.stream()
                        .map(lesson ->
                                lesson.getId())
                        .toList());

        assertFalse(
                lessons.stream()
                        .anyMatch(lesson ->
                                lesson.getId()
                                        .equals(
                                                LESSON_AT_UPPER_BOUND_ID)));
    }

    @Test
    @DisplayName("findCourseSchedule: returns course lessons across groups inside time window")
    void findCourseSchedule_returnsCourseLessonsAcrossGroups() {

        var lessons =
                lessonRepository.findCourseSchedule(
                        COURSE_1_ID,
                        SCHEDULE_FROM,
                        SCHEDULE_TO);

        assertEquals(
                List.of(
                        LESSON_BASE_ID,
                        LESSON_TEACHER_OVERLAP_ID),
                lessons.stream()
                        .map(lesson ->
                                lesson.getId())
                        .toList());
    }

    @Test
    @DisplayName("findTeacherSchedule: uses assigned teacher rather than current course teacher")
    void findTeacherSchedule_usesAssignedTeacher() {

        var lessons =
                lessonRepository.findTeacherSchedule(
                        TEACHER_1_ID,
                        SCHEDULE_FROM,
                        SCHEDULE_TO);

        assertEquals(
                List.of(
                        LESSON_BASE_ID,
                        LESSON_TEACHER_OVERLAP_ID),
                lessons.stream()
                        .map(lesson ->
                                lesson.getId())
                        .toList());

        assertFalse(
                lessons.stream()
                        .anyMatch(lesson ->
                                lesson.getId()
                                        .equals(
                                                LESSON_GROUP_OVERLAP_ID)),
                "lesson assigned to another teacher must not be returned even when the course currently belongs to this teacher");
    }

    @Test
    @DisplayName("findTeacherSchedule: missing teacher -> empty list")
    void findTeacherSchedule_missingTeacher_returnsEmptyList() {

        var lessons =
                lessonRepository.findTeacherSchedule(
                        MISSING_ID,
                        SCHEDULE_FROM,
                        SCHEDULE_TO);

        assertTrue(
                lessons.isEmpty());
    }

    @Test
    @DisplayName("findFutureByCourseId: returns only lessons strictly after cutoff")
    void findFutureByCourseId_returnsLessonsStrictlyAfterCutoff() {

        var lessons =
                lessonRepository.findFutureByCourseId(
                        COURSE_1_ID,
                        FUTURE_CUTOFF);

        assertEquals(
                List.of(
                        LESSON_FUTURE_ID),
                lessons.stream()
                        .map(lesson ->
                                lesson.getId())
                        .toList());

        assertFalse(
                lessons.stream()
                        .anyMatch(lesson ->
                                lesson.getId()
                                        .equals(
                                                LESSON_AT_UPPER_BOUND_ID)),
                "lesson starting exactly at the cutoff must not be considered future");
    }
}