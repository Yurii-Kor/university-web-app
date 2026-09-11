package ua.foxminded.university.service.course;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.course.CourseServiceTestConstants.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import ua.foxminded.university.TestClockConfig;
import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.domain.Lesson;
import ua.foxminded.university.model.persistence.lesson.LessonRepository;
import ua.foxminded.university.model.persistence.lesson.projection.OverlapHit;
import ua.foxminded.university.service.course.dto.CourseSelfUpdateDto;
import ua.foxminded.university.service.course.exception.CourseTeacherReassignmentException;

@SpringBootTest
@ActiveProfiles("test")
@Import({
		TestcontainersConfiguration.class,
		TestClockConfig.class
})
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
		scripts = {
				"/sql/course/course-service-base.sql",
				"/sql/course/course-teacher-reassignment.sql"
		},
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
		scripts = "/sql/cleanup-database.sql",
		executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class CourseServiceTeacherReassignmentTest {

	@Autowired
	private CourseService courseService;

	@Autowired
	private LessonRepository lessonRepository;

	@Test
	@DisplayName("updateSelf: teacher change reassigns only future lessons")
	void updateSelf_changeTeacher_reassignsOnlyFutureLessons() {
		var dto = new CourseSelfUpdateDto(
				COURSE_OS_ID,
				null,
				null,
				OTHER_TEACHER_ID);

		courseService.updateSelf(dto);

		var course = courseService.findByIds(
				List.of(COURSE_OS_ID))
				.getFirst();

		var currentTeacherLessonIds =
				lessonIdsForTeacher(
						TEST_TEACHER_ID);

		var targetTeacherLessonIds =
				lessonIdsForTeacher(
						OTHER_TEACHER_ID);

		assertAll(
				() -> assertEquals(
						OTHER_TEACHER_ID,
						course.getTeacher()
								.getId()),

				() -> assertTrue(
						currentTeacherLessonIds.contains(
								OS_PAST_LESSON_ID)),

				() -> assertTrue(
						currentTeacherLessonIds.contains(
								OS_ONGOING_LESSON_ID)),

				() -> assertFalse(
						currentTeacherLessonIds.contains(
								OS_FUTURE_LESSON_1_ID)),

				() -> assertFalse(
						currentTeacherLessonIds.contains(
								OS_FUTURE_LESSON_2_ID)),

				() -> assertTrue(
						targetTeacherLessonIds.contains(
								OS_FUTURE_LESSON_1_ID)),

				() -> assertTrue(
						targetTeacherLessonIds.contains(
								OS_FUTURE_LESSON_2_ID)));
	}

	@Test
	@DisplayName("updateSelf: teacher conflicts abort entire reassignment")
	void updateSelf_teacherConflicts_rollsBackEntireUpdate() {
		var dto = new CourseSelfUpdateDto(
				COURSE_SECURITY_ID,
				CODE_DB_UPPER,
				COURSE_DATABASES,
				OTHER_TEACHER_ID);

		var exception = assertThrows(
				CourseTeacherReassignmentException.class,
				() -> courseService.updateSelf(dto));

		var course = courseService.findByIds(
				List.of(COURSE_SECURITY_ID))
				.getFirst();

		var currentTeacherLessonIds =
				lessonIdsForTeacher(
						TEST_TEACHER_ID);

		var targetTeacherLessonIds =
				lessonIdsForTeacher(
						OTHER_TEACHER_ID);

		var conflictingLessonIds =
				exception.conflicts()
						.stream()
						.map(conflict ->
								conflict.lessonId())
						.toList();

		assertAll(
				() -> assertEquals(
						COURSE_SECURITY_ID,
						exception.courseId()),

				() -> assertEquals(
						TEST_TEACHER_ID,
						exception.currentTeacher()
								.id()),

				() -> assertEquals(
						TEST_TEACHER_FIRST_NAME,
						exception.currentTeacher()
								.firstName()),

				() -> assertEquals(
						TEST_TEACHER_LAST_NAME,
						exception.currentTeacher()
								.lastName()),

				() -> assertEquals(
						TEST_TEACHER_EMAIL,
						exception.currentTeacher()
								.email()),

				() -> assertEquals(
						OTHER_TEACHER_ID,
						exception.targetTeacher()
								.id()),

				() -> assertEquals(
						OTHER_TEACHER_FIRST_NAME,
						exception.targetTeacher()
								.firstName()),

				() -> assertEquals(
						OTHER_TEACHER_LAST_NAME,
						exception.targetTeacher()
								.lastName()),

				() -> assertEquals(
						OTHER_TEACHER_EMAIL,
						exception.targetTeacher()
								.email()),

				() -> assertEquals(
						List.of(
								SECURITY_FUTURE_LESSON_1_ID,
								SECURITY_FUTURE_LESSON_2_ID),
						conflictingLessonIds),

				() -> assertEquals(
						List.of(
								TARGET_TEACHER_LESSON_1_ID),
						conflictIds(
								exception.conflicts()
										.get(0)
										.teacherConflicts())),

				() -> assertEquals(
						List.of(
								TARGET_TEACHER_LESSON_2_ID),
						conflictIds(
								exception.conflicts()
										.get(1)
										.teacherConflicts())),

				() -> assertEquals(
						TEST_TEACHER_ID,
						course.getTeacher()
								.getId()),

				() -> assertEquals(
						CODE_SEC_UPPER,
						course.getCode()),

				() -> assertEquals(
						COURSE_SECURITY,
						course.getName()),

				() -> assertTrue(
						currentTeacherLessonIds.containsAll(
								List.of(
										SECURITY_FUTURE_LESSON_1_ID,
										SECURITY_FUTURE_LESSON_2_ID))),

				() -> assertFalse(
						targetTeacherLessonIds.contains(
								SECURITY_FUTURE_LESSON_1_ID)),

				() -> assertFalse(
						targetTeacherLessonIds.contains(
								SECURITY_FUTURE_LESSON_2_ID)));
	}

	private List<Long> lessonIdsForTeacher(
			long teacherId) {

		return lessonRepository.findTeacherSchedule(
				teacherId,
				REASSIGNMENT_RANGE_START,
				REASSIGNMENT_RANGE_END)
				.stream()
				.map(Lesson::getId)
				.toList();
	}

	private List<Long> conflictIds(
			List<OverlapHit> conflicts) {

		return conflicts.stream()
				.map(OverlapHit::entryId)
				.toList();
	}
}