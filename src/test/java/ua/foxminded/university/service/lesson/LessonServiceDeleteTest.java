package ua.foxminded.university.service.lesson;

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
import ua.foxminded.university.TestClockConfig;
import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.model.persistence.lesson.LessonRepository;

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
class LessonServiceDeleteTest {

	@Autowired
	private LessonService lessonService;

	@Autowired
	private LessonRepository lessonRepository;

	@Test
	@DisplayName("deleteById: owned future lesson -> deletes lesson")
	void deleteById_ownedFutureLesson_success() {
		lessonService.deleteById(
				TEACHER_1_ID,
				UPDATE_TARGET_ID);

		assertTrue(
				lessonRepository.findById(
						UPDATE_TARGET_ID)
						.isEmpty());

		assertEquals(
				BASE_LESSON_COUNT - 1,
				lessonRepository.count());
	}

	@Test
	@DisplayName("deleteById: missing lesson -> EntityNotFoundException")
	void deleteById_missingLesson_fails() {
		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> lessonService.deleteById(
						TEACHER_1_ID,
						MISSING_ID));

		assertEquals(
				"Lesson not found: id="
						+ MISSING_ID,
				exception.getMessage());

		assertEquals(
				BASE_LESSON_COUNT,
				lessonRepository.count());
	}

	@Test
	@DisplayName("deleteById: started lesson -> IllegalStateException")
	void deleteById_startedLesson_fails() {
		var exception = assertThrows(
				IllegalStateException.class,
				() -> lessonService.deleteById(
						TEACHER_1_ID,
						STARTED_LESSON_ID));

		assertEquals(
				"Only future lessons can be modified.",
				exception.getMessage());

		assertTrue(
				lessonRepository.findById(
						STARTED_LESSON_ID)
						.isPresent());

		assertEquals(
				BASE_LESSON_COUNT,
				lessonRepository.count());
	}

	@Test
	@DisplayName("deleteById: lesson assigned to another teacher -> IllegalStateException")
	void deleteById_lessonAssignedToAnotherTeacher_fails() {
	    var exception = assertThrows(
	            IllegalStateException.class,
	            () -> lessonService.deleteById(
	                    TEACHER_1_ID,
	                    ASSIGNED_TEACHER_2_LESSON_ID));

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

	    assertEquals(
	            BASE_LESSON_COUNT,
	            lessonRepository.count());
	}
}