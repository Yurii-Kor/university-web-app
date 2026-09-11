package ua.foxminded.university.service.lesson;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import jakarta.persistence.EntityNotFoundException;
import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.domain.Lesson;
import ua.foxminded.university.model.persistence.lesson.LessonRepository;
import ua.foxminded.university.service.lesson.dto.LessonUpdateDto;
import ua.foxminded.university.service.lesson.exception.ScheduleConflictException;

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
class LessonServiceUpdateTest {

	@Autowired
	private LessonService lessonService;

	@Autowired
	private LessonRepository lessonRepository;

	@Test
	@DisplayName("update: happy path -> updates all lesson fields")
	void update_happyPath_updatesAllFields() {
		var dto = new LessonUpdateDto(
				COURSE_1_ID,
				GROUP_1_ID,
				UPDATED_START,
				UPDATED_END,
				UPDATED_ROOM,
				UPDATED_TYPE,
				UPDATED_DESCRIPTION);

		lessonService.update(
				TEACHER_1_ID,
				UPDATE_TARGET_ID,
				dto);

		var persisted = requireTarget();

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
						UPDATED_START,
						persisted.getStartTime()),
				() -> assertEquals(
						UPDATED_END,
						persisted.getEndTime()),
				() -> assertEquals(
						UPDATED_ROOM,
						persisted.getRoom()),
				() -> assertEquals(
						UPDATED_TYPE,
						persisted.getLessonType()),
				() -> assertEquals(
						UPDATED_DESCRIPTION,
						persisted.getDescription()));
	}

	@Test
	@DisplayName("update: empty patch -> leaves lesson unchanged")
	void update_emptyPatch_keepsLessonUnchanged() {
		lessonService.update(
				TEACHER_1_ID,
				UPDATE_TARGET_ID,
				emptyPatch());

		assertTargetUnchanged();
	}

	@Test
	@DisplayName("update: missing lesson -> EntityNotFoundException")
	void update_missingLesson_fails() {
		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						MISSING_ID,
						emptyPatch()));

		assertEquals(
				"Lesson not found: id="
						+ MISSING_ID,
				exception.getMessage());
	}

	@Test
	@DisplayName("update: started lesson -> IllegalStateException")
	void update_startedLesson_fails() {
		var exception = assertThrows(
				IllegalStateException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						STARTED_LESSON_ID,
						emptyPatch()));

		assertEquals(
				"Only future lessons can be modified.",
				exception.getMessage());
	}

	@Test
	@DisplayName("update: lesson assigned to another teacher -> IllegalStateException")
	void update_lessonAssignedToAnotherTeacher_fails() {
	    var exception = assertThrows(
	            IllegalStateException.class,
	            () -> lessonService.update(
	                    TEACHER_1_ID,
	                    ASSIGNED_TEACHER_2_LESSON_ID,
	                    emptyPatch()));

	    assertEquals(
	            "Lesson does not belong to the teacher: lessonId="
	                    + ASSIGNED_TEACHER_2_LESSON_ID
	                    + ", teacherId="
	                    + TEACHER_1_ID,
	            exception.getMessage());

	    assertTrue(
	            lessonRepository.findById(
	                    ASSIGNED_TEACHER_2_LESSON_ID)
	                    .isPresent());
	}

	@Test
	@DisplayName("update: missing new course -> EntityNotFoundException")
	void update_missingCourse_fails() {
		var dto = new LessonUpdateDto(
				MISSING_ID,
				null,
				null,
				null,
				null,
				null,
				null);

		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						UPDATE_TARGET_ID,
						dto));

		assertEquals(
				"Course not found: id="
						+ MISSING_ID,
				exception.getMessage());

		assertTargetUnchanged();
	}

	@Test
	@DisplayName("update: missing new group -> EntityNotFoundException")
	void update_missingGroup_fails() {
		var dto = new LessonUpdateDto(
				null,
				MISSING_ID,
				null,
				null,
				null,
				null,
				null);

		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						UPDATE_TARGET_ID,
						dto));

		assertEquals(
				"StudyGroup not found: id="
						+ MISSING_ID,
				exception.getMessage());

		assertTargetUnchanged();
	}

	@Test
	@DisplayName("update: switch to foreign course -> IllegalStateException")
	void update_switchToForeignCourse_fails() {
		var dto = new LessonUpdateDto(
				COURSE_3_ID,
				null,
				null,
				null,
				null,
				null,
				null);

		var exception = assertThrows(
				IllegalStateException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						UPDATE_TARGET_ID,
						dto));

		assertEquals(
				"Course does not belong to the teacher: courseId="
						+ COURSE_3_ID
						+ ", teacherId="
						+ TEACHER_1_ID,
				exception.getMessage());

		assertTargetUnchanged();
	}

	@Test
	@DisplayName("update: group is not assigned to course -> IllegalStateException")
	void update_groupNotAssignedToCourse_fails() {
		var dto = new LessonUpdateDto(
				null,
				GROUP_3_ID,
				null,
				null,
				null,
				null,
				null);

		var exception = assertThrows(
				IllegalStateException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						UPDATE_TARGET_ID,
						dto));

		assertEquals(
				"Study group is not assigned to the course: groupId="
						+ GROUP_3_ID
						+ ", courseId="
						+ COURSE_2_ID,
				exception.getMessage());

		assertTargetUnchanged();
	}

	@Test
	@DisplayName("update: start time equals current time -> IllegalStateException")
	void update_startTimeEqualsNow_fails() {
		var dto = new LessonUpdateDto(
				null,
				null,
				TEST_NOW,
				null,
				null,
				null,
				null);

		var exception = assertThrows(
				IllegalStateException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						UPDATE_TARGET_ID,
						dto));

		assertEquals(
				"Lesson start time must be in the future.",
				exception.getMessage());

		assertTargetUnchanged();
	}

	@Test
	@DisplayName("update: end time equals start time -> IllegalStateException")
	void update_endTimeEqualsStartTime_fails() {
		var dto = new LessonUpdateDto(
				null,
				null,
				null,
				UPDATE_TARGET_START,
				null,
				null,
				null);

		var exception = assertThrows(
				IllegalStateException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						UPDATE_TARGET_ID,
						dto));

		assertEquals(
				"Lesson end time must be after start time.",
				exception.getMessage());

		assertTargetUnchanged();
	}

	@Test
	@DisplayName("update: room is teacher office -> IllegalStateException")
	void update_roomIsTeacherOffice_fails() {
		var dto = new LessonUpdateDto(
				null,
				null,
				null,
				null,
				TEACHER_1_OFFICE,
				null,
				null);

		var exception = assertThrows(
				IllegalStateException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						UPDATE_TARGET_ID,
						dto));

		assertEquals(
				"Room is reserved as a teacher office: room="
						+ TEACHER_1_OFFICE,
				exception.getMessage());

		assertTargetUnchanged();
	}

	@Test
	@DisplayName("update: group overlap -> ScheduleConflictException")
	void update_groupOverlap_fails() {
		var dto = new LessonUpdateDto(
				COURSE_1_ID,
				GROUP_1_ID,
				GROUP_CONFLICT_START,
				GROUP_CONFLICT_END,
				UPDATED_ROOM,
				null,
				null);

		var exception = assertThrows(
				ScheduleConflictException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						UPDATE_TARGET_ID,
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

		assertTargetUnchanged();
	}

	@Test
	@DisplayName("update: teacher overlap -> ScheduleConflictException")
	void update_teacherOverlap_fails() {
		var dto = new LessonUpdateDto(
				COURSE_1_ID,
				GROUP_1_ID,
				TEACHER_CONFLICT_START,
				TEACHER_CONFLICT_END,
				UPDATED_ROOM,
				null,
				null);

		var exception = assertThrows(
				ScheduleConflictException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						UPDATE_TARGET_ID,
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

		assertTargetUnchanged();
	}

	@Test
	@DisplayName("update: room overlap -> ScheduleConflictException")
	void update_roomOverlap_fails() {
		var dto = new LessonUpdateDto(
				COURSE_1_ID,
				GROUP_1_ID,
				ROOM_CONFLICT_START,
				ROOM_CONFLICT_END,
				ROOM_CONFLICT,
				null,
				null);

		var exception = assertThrows(
				ScheduleConflictException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						UPDATE_TARGET_ID,
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

		assertTargetUnchanged();
	}

	private LessonUpdateDto emptyPatch() {
		return new LessonUpdateDto(
				null,
				null,
				null,
				null,
				null,
				null,
				null);
	}

	private Lesson requireTarget() {
		return lessonRepository.findById(
				UPDATE_TARGET_ID)
				.orElseThrow();
	}

	private void assertTargetUnchanged() {
		var persisted = requireTarget();

		assertAll(
				() -> assertEquals(
						COURSE_2_ID,
						persisted.getCourse()
								.getId()),
				() -> assertEquals(
						GROUP_2_ID,
						persisted.getGroup()
								.getId()),
				() -> assertEquals(
						UPDATE_TARGET_START,
						persisted.getStartTime()),
				() -> assertEquals(
						UPDATE_TARGET_END,
						persisted.getEndTime()),
				() -> assertEquals(
						UPDATE_TARGET_ROOM,
						persisted.getRoom()),
				() -> assertEquals(
						UPDATE_TARGET_TYPE,
						persisted.getLessonType()),
				() -> assertEquals(
						UPDATE_TARGET_DESCRIPTION,
						persisted.getDescription()));
	}
}