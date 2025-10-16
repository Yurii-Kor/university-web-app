package ua.foxminded.university.model.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ua.foxminded.university.model.domain.Course;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.domain.validation.config.ValidatorConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class CourseValidatorTest {

	private static final String CASE_PATTERN = "code: \"{0}\", name: \"{1}\" -> valid={2}";
	private static final String NULL = "__NULL__";

	private static final String VALID_CODE_LONG = "CSE-ALG-101";
	private static final String VALID_CODE_SHORT = "SEC-303";
	private static final String VALID_NAME = "Operating Systems (Linux)";

	private static final String EMPTY = "";
	private static final String LOWER = "cse-alg-101";
	private static final String BAD_NUM_2DIG = "CSE-ALG-10";
	private static final String BAD_NUM_4DIG = "CSE-ALG-1000";
	private static final String BAD_PREFIX = "C-ALG-101";
	private static final String BAD_MIDLONG = "CSE-ALGORITHMS-101";

	private static final String BLANK_NAME = " ";
	private static final String TOO_LONG_NAME = "A".repeat(256);

	private static final Teacher TEACHER_STUB = Teacher.builder().id(1L).build();

	@Autowired
	private Validator validator;

	@ParameterizedTest(name = CASE_PATTERN)
	@MethodSource("cases")
	@DisplayName("Course entity bean validation")
	void validateCourse(String code, String name, boolean shouldPass) {
		String effectiveCode = NULL.equals(code) ? null : code;
		String effectiveName = NULL.equals(name) ? null : name;

		Course course = Course.builder().id(1L).code(effectiveCode).name(effectiveName).teacher(TEACHER_STUB).build();

		Set<ConstraintViolation<Course>> violations = validator.validate(course);

		if (shouldPass) {
			assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
		} else {
			assertFalse(violations.isEmpty(), "Expected violations, but got none");
		}
	}

	static Stream<Arguments> cases() {
		return Stream.of(Arguments.of(VALID_CODE_LONG, VALID_NAME, true),
				Arguments.of(VALID_CODE_SHORT, VALID_NAME, true),

				Arguments.of(NULL, VALID_NAME, false),
				Arguments.of(EMPTY, VALID_NAME, false),
				Arguments.of(LOWER, VALID_NAME, false),
				Arguments.of(BAD_NUM_2DIG, VALID_NAME, false),
				Arguments.of(BAD_NUM_4DIG, VALID_NAME, false),
				Arguments.of(BAD_PREFIX, VALID_NAME, false),
				Arguments.of(BAD_MIDLONG, VALID_NAME, false),

				Arguments.of(VALID_CODE_LONG, NULL, false),
				Arguments.of(VALID_CODE_LONG, EMPTY, false),
				Arguments.of(VALID_CODE_LONG, BLANK_NAME, false),
				Arguments.of(VALID_CODE_LONG, TOO_LONG_NAME, false));
	}
}
