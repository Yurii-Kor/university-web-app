package ua.foxminded.university.service.lesson;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ua.foxminded.university.TestClockConfig.TEST_NOW;
import static ua.foxminded.university.service.lesson.LessonServiceTestConstants.*;

import java.time.OffsetDateTime;
import java.util.Objects;

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
import ua.foxminded.university.model.domain.Lesson;
import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.model.persistence.lesson.LessonRepository;
import ua.foxminded.university.service.lesson.dto.LessonUpdateDto;
import ua.foxminded.university.service.lesson.exception.LessonUpdateException;
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
class LessonServiceUpdateTest {

	@Autowired
	private LessonService lessonService;

	@Autowired
	private LessonRepository lessonRepository;

	@Test
	@DisplayName("update: happy path -> updates editable fields and keeps course/group")
	void update_happyPath_updatesEditableFieldsAndKeepsContext() {

		var dto = validUpdateDto();

		lessonService.update(
				TEACHER_1_ID,
				UPDATE_TARGET_ID,
				dto);

		var persisted = requireTarget();

		assertAll(
				() -> assertEquals(
						COURSE_2_ID,
						persisted.getCourse().getId()),
				() -> assertEquals(
						GROUP_2_ID,
						persisted.getGroup().getId()),
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
	@DisplayName("update: same state -> succeeds and self overlap is excluded")
	void update_sameState_succeeds() {

		var dto = updateDto(
				UPDATE_TARGET_START,
				UPDATE_TARGET_END,
				UPDATE_TARGET_ROOM,
				UPDATE_TARGET_TYPE,
				UPDATE_TARGET_DESCRIPTION);

		lessonService.update(
				TEACHER_1_ID,
				UPDATE_TARGET_ID,
				dto);

		assertTargetUnchanged();
	}

	@Test
	@DisplayName("update: blank description -> clears description")
	void update_blankDescription_clearsDescription() {

		var dto = updateDto(
				UPDATED_START,
				UPDATED_END,
				UPDATED_ROOM,
				UPDATED_TYPE,
				"   ");

		lessonService.update(
				TEACHER_1_ID,
				UPDATE_TARGET_ID,
				dto);

		assertNull(
				requireTarget().getDescription());
	}

	@Test
	@DisplayName("update: missing lesson -> EntityNotFoundException")
	void update_missingLesson_fails() {

		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						MISSING_ID,
						validUpdateDto()));

		assertEquals(
				"Lesson not found: id=" + MISSING_ID,
				exception.getMessage());
	}

	@Test
	@DisplayName("update: started lesson -> LessonUpdateException")
	void update_startedLesson_fails() {

		var dto = validUpdateDto();

		var exception = assertThrows(
				LessonUpdateException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						STARTED_LESSON_ID,
						dto));

		assertUpdateViolation(
				exception,
				STARTED_LESSON_ID,
				dto,
				null,
				"Only future lessons can be modified.");
	}

	@Test
	@DisplayName("update: lesson assigned to another teacher -> LessonUpdateException")
	void update_lessonAssignedToAnotherTeacher_fails() {

		var dto = validUpdateDto();

		var exception = assertThrows(
				LessonUpdateException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						ASSIGNED_TEACHER_2_LESSON_ID,
						dto));

		assertUpdateViolation(
				exception,
				ASSIGNED_TEACHER_2_LESSON_ID,
				dto,
				null,
				"Lesson does not belong to the teacher: lessonId=%d, teacherId=%d"
						.formatted(
								ASSIGNED_TEACHER_2_LESSON_ID,
								TEACHER_1_ID));

		assertTrue(
				lessonRepository.findById(
						ASSIGNED_TEACHER_2_LESSON_ID)
						.isPresent());
	}

	@Test
	@DisplayName("update: current course no longer belongs to assigned teacher -> LessonUpdateException")
	void update_courseNoLongerBelongsToAssignedTeacher_fails() {

		var dto = validUpdateDto();

		var exception = assertThrows(
				LessonUpdateException.class,
				() -> lessonService.update(
						TEACHER_2_ID,
						ASSIGNED_TEACHER_2_LESSON_ID,
						dto));

		assertUpdateViolation(
				exception,
				ASSIGNED_TEACHER_2_LESSON_ID,
				dto,
				null,
				"Course does not belong to the teacher: courseId=%d, teacherId=%d"
						.formatted(
								COURSE_1_ID,
								TEACHER_2_ID));

		assertTrue(
				lessonRepository.findById(
						ASSIGNED_TEACHER_2_LESSON_ID)
						.isPresent());
	}

	@Test
	@DisplayName("update: start time equals current time -> LessonUpdateException")
	void update_startTimeEqualsNow_fails() {

		var dto = new LessonUpdateDto(
				TEST_NOW.toLocalDate(),
				TEST_NOW.toLocalTime(),
				TEST_NOW.plusHours(1).toLocalTime(),
				UPDATED_ROOM,
				UPDATED_TYPE,
				UPDATED_DESCRIPTION);

		var exception = assertThrows(
				LessonUpdateException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						UPDATE_TARGET_ID,
						dto));

		assertUpdateViolation(
				exception,
				UPDATE_TARGET_ID,
				dto,
				"startTime",
				"Lesson start time must be in the future.");

		assertTargetUnchanged();
	}

	@Test
	@DisplayName("update: end time equals start time -> LessonUpdateException")
	void update_endTimeEqualsStartTime_fails() {

		var dto = new LessonUpdateDto(
				UPDATE_TARGET_START.toLocalDate(),
				UPDATE_TARGET_START.toLocalTime(),
				UPDATE_TARGET_START.toLocalTime(),
				UPDATED_ROOM,
				UPDATED_TYPE,
				UPDATED_DESCRIPTION);

		var exception = assertThrows(
				LessonUpdateException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						UPDATE_TARGET_ID,
						dto));

		assertUpdateViolation(
				exception,
				UPDATE_TARGET_ID,
				dto,
				"endTime",
				"Lesson end time must be after start time.");

		assertTargetUnchanged();
	}

	@Test
	@DisplayName("update: room is teacher office -> LessonUpdateException")
	void update_roomIsTeacherOffice_fails() {

		var dto = updateDto(
				UPDATED_START,
				UPDATED_END,
				TEACHER_1_OFFICE,
				UPDATED_TYPE,
				UPDATED_DESCRIPTION);

		var exception = assertThrows(
				LessonUpdateException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						UPDATE_TARGET_ID,
						dto));

		assertUpdateViolation(
				exception,
				UPDATE_TARGET_ID,
				dto,
				"room",
				"Room is reserved as a teacher office: room="
						+ TEACHER_1_OFFICE);

		assertTargetUnchanged();
	}

	@Test
	@DisplayName("update: same group and teacher overlap -> ScheduleConflictException")
	void update_groupAndTeacherOverlap_fails() {

		var dto = updateDto(
				TEACHER_CONFLICT_START,
				TEACHER_CONFLICT_END,
				UPDATED_ROOM,
				UPDATED_TYPE,
				UPDATED_DESCRIPTION);

		var exception = assertThrows(
				ScheduleConflictException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						UPDATE_TARGET_ID,
						dto));

		assertAll(
				() -> assertEquals(
						1,
						exception.getGroupConflicts().size()),
				() -> assertEquals(
						1,
						exception.getTeacherConflicts().size()),
				() -> assertTrue(
						exception.getRoomConflicts().isEmpty()),
				() -> assertEquals(
						TEACHER_CONFLICT_LESSON_ID,
						exception.getGroupConflicts()
								.getFirst()
								.entryId()),
				() -> assertEquals(
						TEACHER_CONFLICT_LESSON_ID,
						exception.getTeacherConflicts()
								.getFirst()
								.entryId()));

		assertTargetUnchanged();
	}

	@Test
	@DisplayName("update: room overlap -> ScheduleConflictException")
	void update_roomOverlap_fails() {

		var dto = updateDto(
				ROOM_CONFLICT_START,
				ROOM_CONFLICT_END,
				ROOM_CONFLICT,
				UPDATED_TYPE,
				UPDATED_DESCRIPTION);

		var exception = assertThrows(
				ScheduleConflictException.class,
				() -> lessonService.update(
						TEACHER_1_ID,
						UPDATE_TARGET_ID,
						dto));

		assertAll(
				() -> assertTrue(
						exception.getGroupConflicts().isEmpty()),
				() -> assertTrue(
						exception.getTeacherConflicts().isEmpty()),
				() -> assertEquals(
						1,
						exception.getRoomConflicts().size()),
				() -> assertEquals(
						ROOM_CONFLICT_LESSON_ID,
						exception.getRoomConflicts()
								.getFirst()
								.entryId()));

		assertTargetUnchanged();
	}

	private LessonUpdateDto validUpdateDto() {
		return updateDto(
				UPDATED_START,
				UPDATED_END,
				UPDATED_ROOM,
				UPDATED_TYPE,
				UPDATED_DESCRIPTION);
	}

	private LessonUpdateDto updateDto(
			OffsetDateTime start,
			OffsetDateTime end,
			String room,
			LessonType lessonType,
			String description) {

		return new LessonUpdateDto(
				start.toLocalDate(),
				start.toLocalTime(),
				end.toLocalTime(),
				room,
				lessonType,
				description);
	}

	private void assertUpdateViolation(
			LessonUpdateException exception,
			long lessonId,
			LessonUpdateDto form,
			String field,
			String message) {

		assertAll(
				() -> assertEquals(
						"Lesson cannot be updated.",
						exception.getMessage()),
				() -> assertEquals(
						lessonId,
						exception.getLessonId()),
				() -> assertEquals(
						form,
						exception.getForm()),
				() -> assertTrue(
						exception.getViolations()
								.stream()
								.anyMatch(violation ->
										Objects.equals(
												field,
												violation.field())
												&& message.equals(
														violation.message())),
						"Expected policy violation: field="
								+ field
								+ ", message="
								+ message));
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
						persisted.getCourse().getId()),
				() -> assertEquals(
						GROUP_2_ID,
						persisted.getGroup().getId()),
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