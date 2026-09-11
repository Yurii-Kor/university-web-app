package ua.foxminded.university.service.lesson;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ua.foxminded.university.service.lesson.LessonServiceTestConstants.*;
import static ua.foxminded.university.TestClockConfig.TEST_NOW;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import jakarta.persistence.EntityNotFoundException;
import ua.foxminded.university.TestClockConfig;
import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.model.persistence.lesson.LessonRepository;
import ua.foxminded.university.service.lesson.dto.LessonCreateDto;
import ua.foxminded.university.service.lesson.exception.ScheduleConflictException;

@SpringBootTest
@ActiveProfiles("test")
@Import({
		TestcontainersConfiguration.class,
		TestClockConfig.class
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
class LessonServiceCreateTest {

	@Autowired
	private LessonService lessonService;

	@Autowired
	private LessonRepository lessonRepository;

	@Test
	@DisplayName("create: happy path — creates lesson")
	void create_happyPath_success() {
		var dto = createDto(
				COURSE_1_ID,
				GROUP_1_ID,
				HAPPY_START,
				HAPPY_END,
				CREATE_ROOM,
				CREATE_TYPE,
				CREATE_DESCRIPTION);

		var created = lessonService.create(
				TEACHER_1_ID,
				dto);

		assertNotNull(
				created.getId());

		var persisted = lessonRepository.findById(
				created.getId())
				.orElseThrow();

		assertAll(
				() -> assertEquals(
						COURSE_1_ID,
						persisted.getCourse()
								.getId()),
				() -> assertEquals(
						GROUP_1_ID,
						persisted.getGroup()
								.getId()),
				() -> assertEquals(
						HAPPY_START,
						persisted.getStartTime()),
				() -> assertEquals(
						HAPPY_END,
						persisted.getEndTime()),
				() -> assertEquals(
						CREATE_ROOM,
						persisted.getRoom()),
				() -> assertEquals(
						CREATE_TYPE,
						persisted.getLessonType()),
				() -> assertEquals(
						CREATE_DESCRIPTION,
						persisted.getDescription()),
				() -> assertEquals(
						BASE_LESSON_COUNT + 1,
						lessonRepository.count()));
	}

	@Test
	@DisplayName("create: null type and blank description — applies defaults")
	void create_nullTypeAndBlankDescription_appliesDefaults() {
		var dto = createDto(
				COURSE_1_ID,
				GROUP_1_ID,
				DEFAULTS_START,
				DEFAULTS_END,
				CREATE_ROOM,
				null,
				"   ");

		var created = lessonService.create(
				TEACHER_1_ID,
				dto);

		var persisted = lessonRepository.findById(
				created.getId())
				.orElseThrow();

		assertAll(
				() -> assertEquals(
						LessonType.OTHER,
						persisted.getLessonType()),
				() -> assertEquals(
						null,
						persisted.getDescription()));
	}

	@Test
	@DisplayName("create: missing course -> EntityNotFoundException")
	void create_missingCourse_fails() {
		var dto = createDto(
				MISSING_ID,
				GROUP_1_ID,
				HAPPY_START,
				HAPPY_END,
				CREATE_ROOM,
				CREATE_TYPE,
				CREATE_DESCRIPTION);

		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> lessonService.create(
						TEACHER_1_ID,
						dto));

		assertEquals(
				"Course not found: id="
						+ MISSING_ID,
				exception.getMessage());

		assertBaseLessonCount();
	}

	@Test
	@DisplayName("create: missing group -> EntityNotFoundException")
	void create_missingGroup_fails() {
		var dto = createDto(
				COURSE_1_ID,
				MISSING_ID,
				HAPPY_START,
				HAPPY_END,
				CREATE_ROOM,
				CREATE_TYPE,
				CREATE_DESCRIPTION);

		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> lessonService.create(
						TEACHER_1_ID,
						dto));

		assertEquals(
				"StudyGroup not found: id="
						+ MISSING_ID,
				exception.getMessage());

		assertBaseLessonCount();
	}

	@Test
	@DisplayName("create: course belongs to another teacher -> IllegalStateException")
	void create_courseNotOwnedByTeacher_fails() {
		var dto = createDto(
				COURSE_3_ID,
				GROUP_3_ID,
				HAPPY_START,
				HAPPY_END,
				CREATE_ROOM,
				CREATE_TYPE,
				CREATE_DESCRIPTION);

		var exception = assertThrows(
				IllegalStateException.class,
				() -> lessonService.create(
						TEACHER_1_ID,
						dto));

		assertEquals(
				"Course does not belong to the teacher: courseId="
						+ COURSE_3_ID
						+ ", teacherId="
						+ TEACHER_1_ID,
				exception.getMessage());

		assertBaseLessonCount();
	}

	@Test
	@DisplayName("create: group is not assigned to course -> IllegalStateException")
	void create_groupNotAssignedToCourse_fails() {
		var dto = createDto(
				COURSE_1_ID,
				GROUP_3_ID,
				HAPPY_START,
				HAPPY_END,
				CREATE_ROOM,
				CREATE_TYPE,
				CREATE_DESCRIPTION);

		var exception = assertThrows(
				IllegalStateException.class,
				() -> lessonService.create(
						TEACHER_1_ID,
						dto));

		assertEquals(
				"Study group is not assigned to the course: groupId="
						+ GROUP_3_ID
						+ ", courseId="
						+ COURSE_1_ID,
				exception.getMessage());

		assertBaseLessonCount();
	}

	@Test
	@DisplayName("create: start time equals current time -> IllegalStateException")
	void create_startTimeEqualsNow_fails() {
		var dto = createDto(
				COURSE_1_ID,
				GROUP_1_ID,
				TEST_NOW,
				TEST_NOW.plusHours(1),
				CREATE_ROOM,
				CREATE_TYPE,
				CREATE_DESCRIPTION);

		var exception = assertThrows(
				IllegalStateException.class,
				() -> lessonService.create(
						TEACHER_1_ID,
						dto));

		assertEquals(
				"Lesson start time must be in the future.",
				exception.getMessage());

		assertBaseLessonCount();
	}

	@Test
	@DisplayName("create: end time equals start time -> IllegalStateException")
	void create_endTimeEqualsStartTime_fails() {
		var dto = createDto(
				COURSE_1_ID,
				GROUP_1_ID,
				HAPPY_START,
				HAPPY_START,
				CREATE_ROOM,
				CREATE_TYPE,
				CREATE_DESCRIPTION);

		var exception = assertThrows(
				IllegalStateException.class,
				() -> lessonService.create(
						TEACHER_1_ID,
						dto));

		assertEquals(
				"Lesson end time must be after start time.",
				exception.getMessage());

		assertBaseLessonCount();
	}

	@Test
	@DisplayName("create: room is teacher office -> IllegalStateException")
	void create_roomIsTeacherOffice_fails() {
		var dto = createDto(
				COURSE_1_ID,
				GROUP_1_ID,
				HAPPY_START,
				HAPPY_END,
				TEACHER_1_OFFICE,
				CREATE_TYPE,
				CREATE_DESCRIPTION);

		var exception = assertThrows(
				IllegalStateException.class,
				() -> lessonService.create(
						TEACHER_1_ID,
						dto));

		assertEquals(
				"Room is reserved as a teacher office: room="
						+ TEACHER_1_OFFICE,
				exception.getMessage());

		assertBaseLessonCount();
	}

	@Test
	@DisplayName("create: group overlap -> ScheduleConflictException")
	void create_groupOverlap_fails() {
		var dto = createDto(
				COURSE_1_ID,
				GROUP_1_ID,
				GROUP_CONFLICT_START,
				GROUP_CONFLICT_END,
				CREATE_ROOM,
				CREATE_TYPE,
				CREATE_DESCRIPTION);

		var exception = assertThrows(
				ScheduleConflictException.class,
				() -> lessonService.create(
						TEACHER_1_ID,
						dto));

		assertAll(
				() -> assertEquals(
						1,
						exception.groupConflicts()
								.size()),
				() -> assertTrue(
						exception.teacherConflicts()
								.isEmpty()),
				() -> assertTrue(
						exception.roomConflicts()
								.isEmpty()),
				() -> assertEquals(
						GROUP_CONFLICT_LESSON_ID,
						exception.groupConflicts()
								.getFirst()
								.entryId()));

		assertBaseLessonCount();
	}

	@Test
	@DisplayName("create: teacher overlap -> ScheduleConflictException")
	void create_teacherOverlap_fails() {
		var dto = createDto(
				COURSE_1_ID,
				GROUP_1_ID,
				TEACHER_CONFLICT_START,
				TEACHER_CONFLICT_END,
				CREATE_ROOM,
				CREATE_TYPE,
				CREATE_DESCRIPTION);

		var exception = assertThrows(
				ScheduleConflictException.class,
				() -> lessonService.create(
						TEACHER_1_ID,
						dto));

		assertAll(
				() -> assertTrue(
						exception.groupConflicts()
								.isEmpty()),
				() -> assertEquals(
						1,
						exception.teacherConflicts()
								.size()),
				() -> assertTrue(
						exception.roomConflicts()
								.isEmpty()),
				() -> assertEquals(
						TEACHER_CONFLICT_LESSON_ID,
						exception.teacherConflicts()
								.getFirst()
								.entryId()));

		assertBaseLessonCount();
	}

	@Test
	@DisplayName("create: room overlap -> ScheduleConflictException")
	void create_roomOverlap_fails() {
		var dto = createDto(
				COURSE_1_ID,
				GROUP_1_ID,
				ROOM_CONFLICT_START,
				ROOM_CONFLICT_END,
				ROOM_CONFLICT,
				CREATE_TYPE,
				CREATE_DESCRIPTION);

		var exception = assertThrows(
				ScheduleConflictException.class,
				() -> lessonService.create(
						TEACHER_1_ID,
						dto));

		assertAll(
				() -> assertTrue(
						exception.groupConflicts()
								.isEmpty()),
				() -> assertTrue(
						exception.teacherConflicts()
								.isEmpty()),
				() -> assertEquals(
						1,
						exception.roomConflicts()
								.size()),
				() -> assertEquals(
						ROOM_CONFLICT_LESSON_ID,
						exception.roomConflicts()
								.getFirst()
								.entryId()));

		assertBaseLessonCount();
	}

	@Test
	@DisplayName("create: adjacent lessons do not overlap")
	void create_adjacentToExistingLessons_success() {
		var dto = createDto(
				COURSE_1_ID,
				GROUP_1_ID,
				ADJACENT_START,
				ADJACENT_END,
				CREATE_ROOM,
				CREATE_TYPE,
				CREATE_DESCRIPTION);

		var created = lessonService.create(
				TEACHER_1_ID,
				dto);

		assertNotNull(
				created.getId());

		assertEquals(
				BASE_LESSON_COUNT + 1,
				lessonRepository.count());
	}
	
	@Test
	@DisplayName("create: lesson assigned to another teacher does not cause teacher overlap")
	void create_lessonAssignedToAnotherTeacher_doesNotCauseTeacherOverlap() {
	    var dto = createDto(
	            COURSE_2_ID,
	            GROUP_2_ID,
	            ASSIGNED_TEACHER_2_OVERLAP_START,
	            ASSIGNED_TEACHER_2_OVERLAP_END,
	            CREATE_ROOM,
	            CREATE_TYPE,
	            CREATE_DESCRIPTION);

	    var created = lessonService.create(
	            TEACHER_1_ID,
	            dto);

	    assertNotNull(
	            created.getId());

	    assertEquals(
	            BASE_LESSON_COUNT + 1,
	            lessonRepository.count());
	}

	private LessonCreateDto createDto(
			long courseId,
			long groupId,
			OffsetDateTime startTime,
			OffsetDateTime endTime,
			String room,
			LessonType lessonType,
			String description) {

		return new LessonCreateDto(
				courseId,
				groupId,
				startTime,
				endTime,
				room,
				lessonType,
				description);
	}

	private void assertBaseLessonCount() {
		assertEquals(
				BASE_LESSON_COUNT,
				lessonRepository.count(),
				"failed create must not persist a new lesson");
	}
}