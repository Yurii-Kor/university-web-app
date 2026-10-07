package ua.foxminded.university.service.course;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.course.CourseServiceTestConstants.*;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.service.course.dto.CourseCreateDto;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
    scripts = "/sql/course/course-service-base.sql",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
    scripts = "/sql/cleanup-database.sql",
    executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class CourseServiceFindTest {

    @Autowired
    private CourseService courseService;

    @Test
    @DisplayName("findByIds: null/empty -> returns empty")
    void findByIds_nullOrEmpty_returnsEmpty() {
        assertTrue(courseService.findByIds(null).isEmpty());
        assertTrue(courseService.findByIds(List.of()).isEmpty());
    }

    @Test
    @DisplayName("findByIds: contains nulls and duplicates -> unique non-null results")
    void findByIds_nullsAndDuplicates_ok() {
        var ids = Arrays.asList(
                COURSE_SECURITY_ID,
                null,
                COURSE_SECURITY_ID);

        var result = courseService.findByIds(ids);

        assertEquals(ONE_COURSE, result.size());
        assertEquals(
                COURSE_SECURITY_ID,
                result.getFirst().getId());
    }

    @Test
    @DisplayName("listCourseCardsForAdmin: returns all course cards")
    void listCourseCardsForAdmin_returnsAll() {
        var cardsPage = courseService.listCourseCardsForAdmin(
                PageRequest.of(0, 10));

        assertNotNull(cardsPage);
        assertFalse(
                cardsPage.isEmpty(),
                "admin list must not be empty");

        var ids = cardsPage.getContent()
                .stream()
                .map(card -> card.id())
                .toList();

        assertTrue(
                ids.contains(COURSE_OS_ID),
                "must contain seeded OS course");

        assertTrue(
                ids.contains(COURSE_SECURITY_ID),
                "must contain seeded Security course");
    }

    @Test
    @DisplayName("listCourseCardsForTeacher: returns only teacher courses")
    void listCourseCardsForTeacher_existingTeacher_returnsOnlyOwnCourses() {
        var otherTeacherCourse = courseService.create(
                new CourseCreateDto(
                        TO_CREATE_CODE,
                        TO_CREATE_NAME,
                        null,
                        OTHER_TEACHER_ID));

        var cardsPage = courseService.listCourseCardsForTeacher(
                TEST_TEACHER_ID,
                PageRequest.of(0, 10));

        assertNotNull(cardsPage);
        assertFalse(cardsPage.isEmpty());

        var ids = cardsPage.getContent()
                .stream()
                .map(card -> card.id())
                .toList();

        assertTrue(
                ids.contains(COURSE_OS_ID),
                "must contain OS course of testTeacher");

        assertTrue(
                ids.contains(COURSE_SECURITY_ID),
                "must contain Security course of testTeacher");

        assertFalse(
                ids.contains(otherTeacherCourse.getId()),
                "must not contain course of another teacher");
    }

    @Test
    @DisplayName("listCourseCardsForTeacher: non-existing teacher id -> empty list")
    void listCourseCardsForTeacher_notFound_returnsEmpty() {
        var cardsPage = courseService.listCourseCardsForTeacher(
                NON_EXISTENT_ID,
                PageRequest.of(0, 10));

        assertNotNull(cardsPage);
        assertTrue(
                cardsPage.isEmpty(),
                "non-existing teacher must return empty list");
    }

    @Test
    @DisplayName("listCourseCardsForStudent: returns courses available for student's group")
    void listCourseCardsForStudent_existingStudent_returnsGroupCourses() {
        var cardsPage = courseService.listCourseCardsForStudent(
                STUDENT_IN_G1_ID,
                PageRequest.of(0, 10));

        assertNotNull(cardsPage);
        assertFalse(cardsPage.isEmpty());

        var ids = cardsPage.getContent()
                .stream()
                .map(card -> card.id())
                .toList();

        assertTrue(
                ids.contains(COURSE_OS_ID),
                "student in g1 must see OS course");

        assertTrue(
                ids.contains(COURSE_SECURITY_ID),
                "student in g1 must see Security course");
    }

    @Test
    @DisplayName("listCourseCardsForStudent: non-existing student id -> empty list")
    void listCourseCardsForStudent_notFound_returnsEmpty() {
        var cardsPage = courseService.listCourseCardsForStudent(
                NON_EXISTENT_ID,
                PageRequest.of(0, 10));

        assertNotNull(cardsPage);
        assertTrue(
                cardsPage.isEmpty(),
                "non-existing student must return empty list");
    }

    @Test
    @DisplayName("getCourseGroupsPage: existing course id -> returns page view with header and assigned/available groups")
    void getCourseGroupsPage_existingId_returnsPage() {
        var page = courseService.getCourseGroupsView(
                COURSE_SECURITY_ID);

        assertNotNull(
                page,
                "page must not be null");

        assertNotNull(
                page.course(),
                "course header must not be null");

        assertEquals(
                GROUP_1_ID,
                page.assigned().getFirst().id(),
                "assigned groups must match seeded data");

        assertEquals(
                GROUP_2_ID,
                page.available().getFirst().id(),
                "available groups must match seeded data");
    }

    @Test
    @DisplayName("getCourseGroupsPage: non-existing course id -> EntityNotFoundException")
    void getCourseGroupsPage_notFound_fails() {
        var exception = assertThrows(
                EntityNotFoundException.class,
                () -> courseService.getCourseGroupsView(NON_EXISTENT_ID));

        assertEquals(
                "Course not found: id=" + NON_EXISTENT_ID,
                exception.getMessage());
    }
}