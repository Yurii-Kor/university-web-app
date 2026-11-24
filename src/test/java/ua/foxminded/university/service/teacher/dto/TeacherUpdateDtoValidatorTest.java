package ua.foxminded.university.service.teacher.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
		ValidatorConfig.class
})
class TeacherUpdateDtoValidatorTest {

	private static final String CASE_VALID =
			"valid -> rank={0}, office={1}";

	private static final String CASE_INVALID =
			"invalid -> rank={0}, office={1}, field={2}";

	private static final String VALID_OFFICE = "B-105";

	private static final String OFFICE_LOWER_LETTER = "b-105";
	private static final String OFFICE_NO_DASH = "B105";
	private static final String OFFICE_TWO_DIGITS = "B-12";
	private static final String OFFICE_FOUR_DIGITS = "B-1000";
	private static final String OFFICE_EMPTY = "";

	private static final AcademicRank SOME_RANK =
			AcademicRank.LECTURER;

	@Autowired
	private Validator validator;

	@ParameterizedTest(name = CASE_VALID)
	@MethodSource("validCases")
	@DisplayName("TeacherUpdateDto: valid payloads produce no violations")
	void validate_validCases_noViolations(
			AcademicRank rank,
			String office) {

		var dto = new TeacherUpdateDto(
				rank,
				office);

		Set<ConstraintViolation<TeacherUpdateDto>> violations =
				validator.validate(dto);

		assertTrue(
				violations.isEmpty(),
				"Expected no violations, but got: "
						+ violations);
	}

	static Stream<Arguments> validCases() {

		return Stream.of(
				Arguments.of(
						null,
						null),

				Arguments.of(
						null,
						VALID_OFFICE),

				Arguments.of(
						SOME_RANK,
						null),

				Arguments.of(
						SOME_RANK,
						VALID_OFFICE));
	}

	@ParameterizedTest(name = CASE_INVALID)
	@MethodSource("invalidCases")
	@DisplayName("TeacherUpdateDto: invalid payloads produce violations on expected field")
	void validate_invalidCases_violationOnExpectedField(
			AcademicRank rank,
			String office,
			String expectedField) {

		var dto = new TeacherUpdateDto(
				rank,
				office);

		Set<ConstraintViolation<TeacherUpdateDto>> violations =
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
				"Expected only one field to violate constraints ('"
						+ expectedField
						+ "'), but got: "
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
						OFFICE_EMPTY,
						"office"),

				Arguments.of(
						null,
						OFFICE_LOWER_LETTER,
						"office"),

				Arguments.of(
						null,
						OFFICE_NO_DASH,
						"office"),

				Arguments.of(
						null,
						OFFICE_TWO_DIGITS,
						"office"),

				Arguments.of(
						null,
						OFFICE_FOUR_DIGITS,
						"office"));
	}
}