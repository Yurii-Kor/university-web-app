package ua.foxminded.university.service.lesson.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class LessonCreateDtoValidatorTest {

	private static final LocalDate DATE =
			LocalDate.of(
					2025,
					1,
					10);

	private static final LocalTime START_TIME =
			LocalTime.of(
					10,
					0);

	private static final LocalTime END_TIME =
			START_TIME.plusHours(1);

	private static final Long CID = 10L;
	private static final Long GID = 20L;

	private static final String ROOM_VALID = "B-105";
	private static final String ROOM_LOWER = "b-105";
	private static final String ROOM_NO_DASH = "B105";
	private static final String ROOM_SHORT_DIG = "B-12";
	private static final String ROOM_EMPTY = "";
	private static final String ROOM_SPACED = "B-105 ";

	@Autowired
	private Validator validator;

	@ParameterizedTest(
			name = "[{index}] valid -> c={0}, g={1}, date={2}, start={3}, end={4}, room=\"{5}\", type={6}, desc={7}")
	@MethodSource("validCases")
	@DisplayName("LessonCreateDto: valid payloads produce no violations")
	void validate_validCases_noViolations(
			Long courseId,
			Long groupId,
			LocalDate date,
			LocalTime startTime,
			LocalTime endTime,
			String room,
			LessonType lessonType,
			String description) {

		var dto =
				new LessonCreateDto(
						courseId,
						groupId,
						date,
						startTime,
						endTime,
						room,
						lessonType,
						description);

		Set<ConstraintViolation<LessonCreateDto>> violations =
				validator.validate(
						dto);

		assertTrue(
				violations.isEmpty(),
				"Expected no violations, but got: "
						+ violations);
	}

	static Stream<Arguments> validCases() {

		return Stream.of(
				Arguments.of(
						CID,
						GID,
						DATE,
						START_TIME,
						END_TIME,
						ROOM_VALID,
						LessonType.LECTURE,
						"note"),
				Arguments.of(
						CID,
						GID,
						DATE,
						START_TIME,
						END_TIME,
						ROOM_VALID,
						null,
						"note"),
				Arguments.of(
						CID,
						GID,
						DATE,
						START_TIME,
						END_TIME,
						ROOM_VALID,
						null,
						null),
				Arguments.of(
						CID,
						GID,
						DATE,
						END_TIME,
						START_TIME,
						ROOM_VALID,
						null,
						"note"));
	}

	@ParameterizedTest(
			name = "[{index}] invalid -> c={0}, g={1}, date={2}, start={3}, end={4}, room=\"{5}\" -> field={6}")
	@MethodSource("invalidCases")
	@DisplayName("LessonCreateDto: invalid payloads produce violations on expected field")
	void validate_invalidCases_violationOnExpectedField(
			Long courseId,
			Long groupId,
			LocalDate date,
			LocalTime startTime,
			LocalTime endTime,
			String room,
			String expectedField) {

		var dto =
				new LessonCreateDto(
						courseId,
						groupId,
						date,
						startTime,
						endTime,
						room,
						null,
						"note");

		Set<ConstraintViolation<LessonCreateDto>> violations =
				validator.validate(
						dto);

		assertFalse(
				violations.isEmpty(),
				"Expected violations for field '"
						+ expectedField
						+ "', but got none");

		var fields =
				violations.stream()
						.map(violation ->
								violation.getPropertyPath()
										.toString())
						.collect(
								Collectors.toSet());

		assertEquals(
				1,
				fields.size(),
				"Expected violations only for field '"
						+ expectedField
						+ "', but got fields: "
						+ fields);

		assertEquals(
				expectedField,
				fields.iterator()
						.next(),
				"Violations must be on field '"
						+ expectedField
						+ "'");
	}

	static Stream<Arguments> invalidCases() {

		return Stream.of(
				Arguments.of(
						null,
						GID,
						DATE,
						START_TIME,
						END_TIME,
						ROOM_VALID,
						"courseId"),
				Arguments.of(
						CID,
						null,
						DATE,
						START_TIME,
						END_TIME,
						ROOM_VALID,
						"groupId"),
				Arguments.of(
						CID,
						GID,
						null,
						START_TIME,
						END_TIME,
						ROOM_VALID,
						"date"),
				Arguments.of(
						CID,
						GID,
						DATE,
						null,
						END_TIME,
						ROOM_VALID,
						"startTime"),
				Arguments.of(
						CID,
						GID,
						DATE,
						START_TIME,
						null,
						ROOM_VALID,
						"endTime"),
				Arguments.of(
						CID,
						GID,
						DATE,
						START_TIME,
						END_TIME,
						null,
						"room"),
				Arguments.of(
						CID,
						GID,
						DATE,
						START_TIME,
						END_TIME,
						ROOM_EMPTY,
						"room"),
				Arguments.of(
						CID,
						GID,
						DATE,
						START_TIME,
						END_TIME,
						ROOM_LOWER,
						"room"),
				Arguments.of(
						CID,
						GID,
						DATE,
						START_TIME,
						END_TIME,
						ROOM_NO_DASH,
						"room"),
				Arguments.of(
						CID,
						GID,
						DATE,
						START_TIME,
						END_TIME,
						ROOM_SHORT_DIG,
						"room"),
				Arguments.of(
						CID,
						GID,
						DATE,
						START_TIME,
						END_TIME,
						ROOM_SPACED,
						"room"));
	}
}