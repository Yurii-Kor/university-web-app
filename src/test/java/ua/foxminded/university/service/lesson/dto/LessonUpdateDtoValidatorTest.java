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
class LessonUpdateDtoValidatorTest {

	private static final LocalDate DATE =
			LocalDate.of(2030, 1, 11);

	private static final LocalTime START_TIME =
			LocalTime.of(10, 0);

	private static final LocalTime END_TIME =
			LocalTime.of(11, 0);

	private static final String ROOM_VALID = "B-105";
	private static final String ROOM_LOWER = "b-105";
	private static final String ROOM_EMPTY = "";
	private static final String ROOM_SPACED = "B-105 ";

	private static final LessonType TYPE =
			LessonType.LAB;

	@Autowired
	private Validator validator;

	@ParameterizedTest(
			name = "[{index}] valid -> date={0}, start={1}, end={2}, room=\"{3}\", type={4}, desc={5}")
	@MethodSource("validCases")
	@DisplayName("LessonUpdateDto: valid payloads produce no violations")
	void validate_validCases_noViolations(
			LocalDate date,
			LocalTime startTime,
			LocalTime endTime,
			String room,
			LessonType lessonType,
			String description) {

		var dto = new LessonUpdateDto(
				date,
				startTime,
				endTime,
				room,
				lessonType,
				description);

		Set<ConstraintViolation<LessonUpdateDto>> violations =
				validator.validate(dto);

		assertTrue(
				violations.isEmpty(),
				"Expected no violations, but got: "
						+ violations);
	}

	static Stream<Arguments> validCases() {
		return Stream.of(
				Arguments.of(
						DATE,
						START_TIME,
						END_TIME,
						ROOM_VALID,
						TYPE,
						null),
				Arguments.of(
						DATE,
						START_TIME,
						END_TIME,
						ROOM_VALID,
						TYPE,
						"note"),
				Arguments.of(
						DATE,
						START_TIME,
						END_TIME,
						ROOM_VALID,
						TYPE,
						"   "),
				Arguments.of(
						DATE,
						END_TIME,
						START_TIME,
						ROOM_VALID,
						TYPE,
						"policy validates time order"));
	}

	@ParameterizedTest(
			name = "[{index}] invalid -> date={0}, start={1}, end={2}, room=\"{3}\", type={4} -> field={5}")
	@MethodSource("invalidCases")
	@DisplayName("LessonUpdateDto: invalid payloads produce violations on expected field")
	void validate_invalidCases_violationOnExpectedField(
			LocalDate date,
			LocalTime startTime,
			LocalTime endTime,
			String room,
			LessonType lessonType,
			String expectedField) {

		var dto = new LessonUpdateDto(
				date,
				startTime,
				endTime,
				room,
				lessonType,
				null);

		Set<ConstraintViolation<LessonUpdateDto>> violations =
				validator.validate(dto);

		assertFalse(
				violations.isEmpty(),
				"Expected violations for field '"
						+ expectedField
						+ "', but got none");

		var fields = violations.stream()
				.map(violation ->
						violation.getPropertyPath()
								.toString())
				.collect(Collectors.toSet());

		assertEquals(
				Set.of(expectedField),
				fields,
				"Violations must only be on field '"
						+ expectedField
						+ "'");
	}

	static Stream<Arguments> invalidCases() {
		return Stream.of(
				Arguments.of(
						null,
						START_TIME,
						END_TIME,
						ROOM_VALID,
						TYPE,
						"date"),
				Arguments.of(
						DATE,
						null,
						END_TIME,
						ROOM_VALID,
						TYPE,
						"startTime"),
				Arguments.of(
						DATE,
						START_TIME,
						null,
						ROOM_VALID,
						TYPE,
						"endTime"),
				Arguments.of(
						DATE,
						START_TIME,
						END_TIME,
						null,
						TYPE,
						"room"),
				Arguments.of(
						DATE,
						START_TIME,
						END_TIME,
						ROOM_EMPTY,
						TYPE,
						"room"),
				Arguments.of(
						DATE,
						START_TIME,
						END_TIME,
						ROOM_LOWER,
						TYPE,
						"room"),
				Arguments.of(
						DATE,
						START_TIME,
						END_TIME,
						ROOM_SPACED,
						TYPE,
						"room"),
				Arguments.of(
						DATE,
						START_TIME,
						END_TIME,
						ROOM_VALID,
						null,
						"lessonType"));
	}
}