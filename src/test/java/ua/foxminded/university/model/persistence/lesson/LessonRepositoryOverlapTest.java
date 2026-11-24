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
import ua.foxminded.university.model.persistence.lesson.projection.OverlapHit;

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
class LessonRepositoryOverlapTest {

    @Autowired
    private LessonRepository lessonRepository;

    @Test
    @DisplayName("findGroupOverlaps: returns all overlapping lessons for group ordered by start")
    void findGroupOverlaps_returnsOverlappingLessons() {

        var result =
                lessonRepository.findGroupOverlaps(
                        GROUP_1_ID,
                        GROUP_OVERLAP_QUERY_START,
                        GROUP_OVERLAP_QUERY_END);

        assertEquals(
                List.of(
                        baseHit(),
                        groupOverlapHit()),
                result);
    }

    @Test
    @DisplayName("findTeacherOverlaps: uses assigned teacher and returns overlapping lessons")
    void findTeacherOverlaps_usesAssignedTeacher() {

        var result =
                lessonRepository.findTeacherOverlaps(
                        TEACHER_1_ID,
                        CONFLICT_QUERY_START,
                        CONFLICT_QUERY_END);

        assertEquals(
                List.of(
                        baseHit(),
                        teacherOverlapHit()),
                result);

        assertFalse(
                result.stream()
                        .anyMatch(hit ->
                                hit.entryId()
                                        .equals(
                                                LESSON_GROUP_OVERLAP_ID)),
                "lesson assigned to another teacher must not conflict with the course owner");
    }

    @Test
    @DisplayName("findRoomOverlaps: room comparison is case-insensitive")
    void findRoomOverlaps_roomComparisonIsCaseInsensitive() {

        var result =
                lessonRepository.findRoomOverlaps(
                        ROOM_BASE.toLowerCase(),
                        CONFLICT_QUERY_START,
                        CONFLICT_QUERY_END);

        assertEquals(
                List.of(
                        baseHit(),
                        roomOverlapHit()),
                result);
    }

    @Test
    @DisplayName("findGroupOverlaps: adjacent lessons are not overlaps")
    void findGroupOverlaps_adjacentLessons_returnsEmptyList() {

        var result =
                lessonRepository.findGroupOverlaps(
                        GROUP_3_ID,
                        ADJACENT_WINDOW_START,
                        ADJACENT_WINDOW_END);

        assertTrue(
                result.isEmpty(),
                "lesson ending at requested start or starting at requested end must not overlap");
    }

    @Test
    @DisplayName("findGroupOverlaps: missing group -> empty list")
    void findGroupOverlaps_missingGroup_returnsEmptyList() {

        var result =
                lessonRepository.findGroupOverlaps(
                        MISSING_ID,
                        CONFLICT_QUERY_START,
                        CONFLICT_QUERY_END);

        assertTrue(
                result.isEmpty());
    }

    private OverlapHit baseHit() {

        return new OverlapHit(
                LESSON_BASE_ID,
                GROUP_1_ID,
                COURSE_1_ID,
                TEACHER_1_ID,
                BASE_START,
                BASE_END,
                ROOM_BASE);
    }

    private OverlapHit groupOverlapHit() {

        return new OverlapHit(
                LESSON_GROUP_OVERLAP_ID,
                GROUP_1_ID,
                COURSE_2_ID,
                TEACHER_2_ID,
                GROUP_OVERLAP_LESSON_START,
                GROUP_OVERLAP_LESSON_END,
                ROOM_GROUP_OVERLAP);
    }

    private OverlapHit teacherOverlapHit() {

        return new OverlapHit(
                LESSON_TEACHER_OVERLAP_ID,
                GROUP_2_ID,
                COURSE_1_ID,
                TEACHER_1_ID,
                TEACHER_OVERLAP_LESSON_START,
                TEACHER_OVERLAP_LESSON_END,
                ROOM_TEACHER_OVERLAP);
    }

    private OverlapHit roomOverlapHit() {

        return new OverlapHit(
                LESSON_ROOM_OVERLAP_ID,
                GROUP_2_ID,
                COURSE_3_ID,
                TEACHER_2_ID,
                ROOM_OVERLAP_LESSON_START,
                ROOM_OVERLAP_LESSON_END,
                ROOM_ROOM_OVERLAP);
    }
}