package ua.foxminded.university.service.dto.request;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class StudentDtoValidatorTest {

    private static final String CASE =
            "email={0}, pwd={1}, first={2}, last={3}, groupId={4}, year={5} -> valid={6}";

    // valid baseline
    private static final String VALID_EMAIL = "student.valid@example.com";
    private static final String VALID_PASSWORD = "Abcd1234!";
    private static final String VALID_FIRST = "Mary-Jane"; 
    private static final String VALID_LAST = "O'Connor"; 
    private static final Long   VALID_GROUP_ID = 1L;
    private static final Integer VALID_YEAR = 2024;

    // non-valid
    private static final String BAD_EMAIL = "not-an-email";
    private static final String SHORT_PWD = "A1!a";
    private static final String PWD_NO_UPPER = "abcd1234!";
    private static final String PWD_NO_LOWER = "ABCD1234!";
    private static final String PWD_NO_DIGIT = "Abcdefg!";
    private static final String PWD_NO_SPECIAL = "Abcdefg1";
    private static final String PWD_WITH_SPACE = "Abcd 1234!";

    private static final String NAME_WITH_DIGIT = "Ann3";
    private static final String NAME_WITH_SYMBOL = "Doe#";
    private static final String NAME_WITH_SPACE = "John Doe";
    private static final String TOO_LONG_NAME = "A".repeat(65);

    private static final String EMAIL_256 = "a".repeat(246) + "@x.com"; 
    private static final String PWD_101   = "A".repeat(50) + "a".repeat(50) + "!"; 

    @Autowired
    private Validator validator;

	@ParameterizedTest(name = CASE)
	@MethodSource("cases")
	@DisplayName("StudentCreateDto: bean validation")
	void validate(String email, String password, String firstName, String lastName, Long groupId,
			Integer enrollmentYear, boolean shouldPass) {

		StudentDto dto = new StudentDto(email, password, firstName, lastName, groupId, enrollmentYear);

		Set<ConstraintViolation<StudentDto>> violations = validator.validate(dto);

		if (shouldPass) {
			assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
		} else {
			assertFalse(violations.isEmpty(), "Expected violations, but got none");
		}
	}

    static Stream<Arguments> cases() {
        return Stream.of(
            // --- valid ---
            Arguments.of(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_GROUP_ID, VALID_YEAR, true),
            Arguments.of(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_GROUP_ID, 2000, true),
            Arguments.of(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_GROUP_ID, 2100, true),
            Arguments.of(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_GROUP_ID, null,  true),

            // --- email ---
            Arguments.of(null,         VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_GROUP_ID, VALID_YEAR, false),
            Arguments.of(BAD_EMAIL,    VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_GROUP_ID, VALID_YEAR, false),
            Arguments.of(EMAIL_256,    VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_GROUP_ID, VALID_YEAR, false), 

            // --- password ---
            Arguments.of(VALID_EMAIL, null,            VALID_FIRST, VALID_LAST, VALID_GROUP_ID, VALID_YEAR, false), 
            Arguments.of(VALID_EMAIL, SHORT_PWD,       VALID_FIRST, VALID_LAST, VALID_GROUP_ID, VALID_YEAR, false),
            Arguments.of(VALID_EMAIL, PWD_NO_UPPER,    VALID_FIRST, VALID_LAST, VALID_GROUP_ID, VALID_YEAR, false),
            Arguments.of(VALID_EMAIL, PWD_NO_LOWER,    VALID_FIRST, VALID_LAST, VALID_GROUP_ID, VALID_YEAR, false),
            Arguments.of(VALID_EMAIL, PWD_NO_DIGIT,    VALID_FIRST, VALID_LAST, VALID_GROUP_ID, VALID_YEAR, false),
            Arguments.of(VALID_EMAIL, PWD_NO_SPECIAL,  VALID_FIRST, VALID_LAST, VALID_GROUP_ID, VALID_YEAR, false),
            Arguments.of(VALID_EMAIL, PWD_WITH_SPACE,  VALID_FIRST, VALID_LAST, VALID_GROUP_ID, VALID_YEAR, false),
            Arguments.of(VALID_EMAIL, PWD_101,         VALID_FIRST, VALID_LAST, VALID_GROUP_ID, VALID_YEAR, false), 

            // --- firstName / lastName ---
            Arguments.of(VALID_EMAIL, VALID_PASSWORD, null,           VALID_LAST, VALID_GROUP_ID, VALID_YEAR, false),
            Arguments.of(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST,    null,       VALID_GROUP_ID, VALID_YEAR, false), 
            Arguments.of(VALID_EMAIL, VALID_PASSWORD, NAME_WITH_DIGIT,VALID_LAST, VALID_GROUP_ID, VALID_YEAR, false),
            Arguments.of(VALID_EMAIL, VALID_PASSWORD, NAME_WITH_SPACE,VALID_LAST, VALID_GROUP_ID, VALID_YEAR, false),
            Arguments.of(VALID_EMAIL, VALID_PASSWORD, TOO_LONG_NAME,  VALID_LAST, VALID_GROUP_ID, VALID_YEAR, false),
            Arguments.of(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST,    NAME_WITH_SYMBOL, VALID_GROUP_ID, VALID_YEAR, false),

            // --- groupId ---
            Arguments.of(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, null, VALID_YEAR, false),

            // --- year ---
            Arguments.of(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_GROUP_ID, 1999, false),
            Arguments.of(VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_GROUP_ID, 2101, false)
        );
    }
}
