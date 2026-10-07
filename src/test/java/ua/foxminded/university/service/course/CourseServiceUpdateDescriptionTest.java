package ua.foxminded.university.service.course;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.course.CourseServiceTestConstants.*;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import ua.foxminded.university.TestcontainersConfiguration;
import ua.foxminded.university.service.course.dto.CourseDescriptionUpdateDto;

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
class CourseServiceUpdateDescriptionTest {

	@Autowired
	private CourseService courseService;

	@Test
	@DisplayName("updateDescription: happy path — updates description and trims")
	void updateDescription_happyPath_success() {
		var dto = new CourseDescriptionUpdateDto(
				COURSE_SECURITY_ID,
				DESC_A);

		courseService.updateDescription(dto);

		var reloaded = courseService.findByIds(
				List.of(COURSE_SECURITY_ID))
				.getFirst();

		assertEquals(
				DESC_A.trim(),
				reloaded.getDescription());
	}

	@Test
	@DisplayName("updateDescription: blank -> normalizes to null")
	void updateDescription_blankToNull_success() {
		courseService.updateDescription(
				new CourseDescriptionUpdateDto(
						COURSE_SECURITY_ID,
						"Some description"));

		courseService.updateDescription(
				new CourseDescriptionUpdateDto(
						COURSE_SECURITY_ID,
						DESC_BLANK));

		var reloaded = courseService.findByIds(
				List.of(COURSE_SECURITY_ID))
				.getFirst();

		assertNull(
				reloaded.getDescription(),
				"Blank description must become null");
	}

	@Test
	@DisplayName("updateDescription: null description -> no-op (keeps existing)")
	void updateDescription_nullDescription_noop() {
		courseService.updateDescription(
				new CourseDescriptionUpdateDto(
						COURSE_SECURITY_ID,
						"Existing"));

		courseService.updateDescription(
				new CourseDescriptionUpdateDto(
						COURSE_SECURITY_ID,
						null));

		var reloaded = courseService.findByIds(
				List.of(COURSE_SECURITY_ID))
				.getFirst();

		assertEquals(
				"Existing",
				reloaded.getDescription(),
				"Null description must not change existing value");
	}

	@Test
	@DisplayName("updateDescription: null id -> ConstraintViolationException")
	void updateDescription_nullId_fails() {
		var dto = new CourseDescriptionUpdateDto(
				null,
				"abc");

		assertThrows(
				ConstraintViolationException.class,
				() -> courseService.updateDescription(dto));
	}

	@Test
	@DisplayName("updateDescription: non-existing id -> EntityNotFoundException")
	void updateDescription_notFound_fails() {
		var dto = new CourseDescriptionUpdateDto(
				NON_EXISTENT_ID,
				"abc");

		assertThrows(
				EntityNotFoundException.class,
				() -> courseService.updateDescription(dto));
	}

	@Test
	@DisplayName("updateDescription: too long -> ConstraintViolationException")
	void updateDescription_tooLong_fails() {
		var dto = new CourseDescriptionUpdateDto(
				COURSE_SECURITY_ID,
				"A".repeat(10_001));

		assertThrows(
				ConstraintViolationException.class,
				() -> courseService.updateDescription(dto));
	}
}