package ua.foxminded.university.service.lesson.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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

	private static final OffsetDateTime NOW =
			OffsetDateTime.of(
					2025,
					1,
					10,
					10,
					0,
					0,
					0,
					ZoneOffset.UTC);

	private static final OffsetDateTime PLUS_1H =
			NOW.plusHours(1);

	private static final Long CID = 10L;
	private static final Long GID = 20L;

	private static final String ROOM_VALID = "B-105";
	private static final String ROOM_LOWER = "b-105";
	private static final String ROOM_EMPTY = "";
	private static final String ROOM_SPACED = "B-105 ";

	@Autowired
	private Validator validator;

	@ParameterizedTest(
			name = "[{index}] valid -> c={0}, g={1}, start={2}, end={3}, room=\"{4}\", type={5}, desc={6}")
	@MethodSource("validCases")
	@DisplayName("LessonUpdateDto: valid payloads produce no violations")
	void validate_validCases_noViolations(
			Long courseId,
			Long groupId,
			OffsetDateTime startTime,
			OffsetDateTime endTime,
			String room,
			LessonType lessonType,
			String description) {

		var dto = new LessonUpdateDto(
				courseId,
				groupId,
				startTime,
				endTime,
				room,
				lessonType,
				description);

		Set<ConstraintViolation<LessonUpdateDto>> violations =
				validator.validate(dto);

		assertTrue(
				violations.isEmpty(),
				"Expected no violations, but got: " + violations);
	}

	static Stream<Arguments> validCases() {
		return Stream.of(
				Arguments.of(
						null,
						null,
						null,
						null,
						null,
						null,
						null),
				Arguments.of(
						CID,
						null,
						null,
						null,
						null,
						null,
						null),
				Arguments.of(
						null,
						GID,
						null,
						null,
						null,
						null,
						null),
				Arguments.of(
						null,
						null,
						NOW,
						null,
						null,
						null,
						null),
				Arguments.of(
						null,
						null,
						null,
						PLUS_1H,
						null,
						null,
						null),
				Arguments.of(
						null,
						null,
						PLUS_1H,
						NOW,
						null,
						null,
						null),
				Arguments.of(
						null,
						null,
						null,
						null,
						ROOM_VALID,
						null,
						null),
				Arguments.of(
						null,
						null,
						null,
						null,
						null,
						LessonType.LAB,
						null),
				Arguments.of(
						null,
						null,
						null,
						null,
						null,
						null,
						"note"));
	}

	@ParameterizedTest(
			name = "[{index}] invalid -> room=\"{4}\" -> field={7}")
	@MethodSource("invalidCases")
	@DisplayName("LessonUpdateDto: invalid payloads produce violations on expected field")
	void validate_invalidCases_violationOnExpectedField(
			Long courseId,
			Long groupId,
			OffsetDateTime startTime,
			OffsetDateTime endTime,
			String room,
			LessonType lessonType,
			String description,
			String expectedField) {

		var dto = new LessonUpdateDto(
				courseId,
				groupId,
				startTime,
				endTime,
				room,
				lessonType,
				description);

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
						null,
						null,
						null,
						ROOM_EMPTY,
						null,
						null,
						"room"),
				Arguments.of(
						null,
						null,
						null,
						null,
						ROOM_LOWER,
						null,
						null,
						"room"),
				Arguments.of(
						null,
						null,
						null,
						null,
						ROOM_SPACED,
						null,
						null,
						"room"));
	}
}