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

import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.validation.config.ValidatorConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class StudyGroupValidatorTest {

	private static final String CASE_PATTERN = "name: \"{0}\" -> valid={1}";
	private static final String NULL = "__NULL__";

	private static final String VALID_NAME = "CS-101";

	private static final String EMPTY = "";
	private static final String LOWERCASE = "cs-101";
	private static final String MISSING_HYPHEN = "CS101";
	private static final String WRONG_PREFIX_LEN = "AAA-101";
	private static final String WRONG_DIGITS_SHORT = "CS-10";
	private static final String WRONG_DIGITS_LONG = "CS-1000";
	private static final String MIXED_CASE = "Cs-101";
	private static final String NON_LETTER = "C1-101";
	private static final String WITH_SPACE = "CS- 101";
	private static final String TRAILING_SPACE = "CS-101 ";

	@Autowired
	private Validator validator;

	@ParameterizedTest(name = CASE_PATTERN)
	@MethodSource("cases")
	@DisplayName("StudyGroup entity bean validation")
	void validateStudyGroup(String name, boolean shouldPass) {
		String effectiveName = NULL.equals(name) ? null : name;

		StudyGroup group = StudyGroup.builder().id(1L).name(effectiveName).build();

		Set<ConstraintViolation<StudyGroup>> violations = validator.validate(group);

		if (shouldPass) {
			assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
		} else {
			assertFalse(violations.isEmpty(), "Expected violations, but got none");
		}
	}

	static Stream<Arguments> cases() {
		return Stream.of(Arguments.of(VALID_NAME, true),

				Arguments.of(NULL, false),
				Arguments.of(EMPTY, false),
				Arguments.of(LOWERCASE, false),
				Arguments.of(MISSING_HYPHEN, false),
				Arguments.of(WRONG_PREFIX_LEN, false),
				Arguments.of(WRONG_DIGITS_SHORT, false),
				Arguments.of(WRONG_DIGITS_LONG, false),
				Arguments.of(MIXED_CASE, false),
				Arguments.of(NON_LETTER, false),
				Arguments.of(WITH_SPACE, false),
				Arguments.of(TRAILING_SPACE, false));
	}
}
