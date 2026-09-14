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
import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView;

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
class LessonRepositoryProjectionTest {

    @Autowired
    private LessonRepository lessonRepository;

    @Test
    @DisplayName("findGroupScheduleItems: returns complete projections ordered by start")
    void findGroupScheduleItems_returnsCompleteProjectionsOrdered() {

        var result =
                lessonRepository.findGroupScheduleItems(
                        GROUP_1_ID,
                        SCHEDULE_FROM,
                        SCHEDULE_TO);

        assertEquals(
                List.of(
                        baseView(),
                        groupOverlapView()),
                result);
    }

    @Test
    @DisplayName("findTeacherScheduleItems: uses assigned teacher and returns complete projections")
    void findTeacherScheduleItems_usesAssignedTeacher() {

        var result =
                lessonRepository.findTeacherScheduleItems(
                        TEACHER_1_ID,
                        SCHEDULE_FROM,
                        SCHEDULE_TO);

        assertEquals(
                List.of(
                        baseView(),
                        teacherOverlapView()),
                result);

        assertFalse(
                result.stream()
                        .anyMatch(item ->
                                item.id()
                                        .equals(
                                                LESSON_GROUP_OVERLAP_ID)),
                "projection query must filter by assigned teacher rather than current course teacher");
    }

    @Test
    @DisplayName("findTeacherScheduleItem: matching assigned teacher -> returns lesson projection")
    void findTeacherScheduleItem_matchingTeacher_returnsProjection() {

        var result =
                lessonRepository.findTeacherScheduleItem(
                        TEACHER_2_ID,
                        LESSON_GROUP_OVERLAP_ID);

        assertTrue(
                result.isPresent());

        assertEquals(
                groupOverlapView(),
                result.orElseThrow());
    }

    @Test
    @DisplayName("findTeacherScheduleItem: different assigned teacher -> empty Optional")
    void findTeacherScheduleItem_differentTeacher_returnsEmpty() {

        var result =
                lessonRepository.findTeacherScheduleItem(
                        TEACHER_1_ID,
                        LESSON_GROUP_OVERLAP_ID);

        assertTrue(
                result.isEmpty());
    }

    @Test
    @DisplayName("findTeacherScheduleItem: missing lesson -> empty Optional")
    void findTeacherScheduleItem_missingLesson_returnsEmpty() {

        var result =
                lessonRepository.findTeacherScheduleItem(
                        TEACHER_1_ID,
                        MISSING_ID);

        assertTrue(
                result.isEmpty());
    }

    private LessonScheduleItemView baseView() {

        return new LessonScheduleItemView(
                LESSON_BASE_ID,
                COURSE_1_ID,
                COURSE_1_CODE,
                COURSE_1_NAME,
                GROUP_1_ID,
                GROUP_1_NAME,
                TEACHER_1_ID,
                TEACHER_1_FIRST_NAME,
                TEACHER_1_LAST_NAME,
                BASE_START,
                BASE_END,
                ROOM_BASE,
                LessonType.LECTURE,
                BASE_DESCRIPTION);
    }

    private LessonScheduleItemView groupOverlapView() {

        return new LessonScheduleItemView(
                LESSON_GROUP_OVERLAP_ID,
                COURSE_2_ID,
                COURSE_2_CODE,
                COURSE_2_NAME,
                GROUP_1_ID,
                GROUP_1_NAME,
                TEACHER_2_ID,
                TEACHER_2_FIRST_NAME,
                TEACHER_2_LAST_NAME,
                GROUP_OVERLAP_LESSON_START,
                GROUP_OVERLAP_LESSON_END,
                ROOM_GROUP_OVERLAP,
                LessonType.PRACTICE,
                GROUP_OVERLAP_DESCRIPTION);
    }

    private LessonScheduleItemView teacherOverlapView() {

        return new LessonScheduleItemView(
                LESSON_TEACHER_OVERLAP_ID,
                COURSE_1_ID,
                COURSE_1_CODE,
                COURSE_1_NAME,
                GROUP_2_ID,
                GROUP_2_NAME,
                TEACHER_1_ID,
                TEACHER_1_FIRST_NAME,
                TEACHER_1_LAST_NAME,
                TEACHER_OVERLAP_LESSON_START,
                TEACHER_OVERLAP_LESSON_END,
                ROOM_TEACHER_OVERLAP,
                LessonType.LAB,
                TEACHER_OVERLAP_DESCRIPTION);
    }
}