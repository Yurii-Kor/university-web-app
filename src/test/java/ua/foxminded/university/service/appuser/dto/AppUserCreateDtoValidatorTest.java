package ua.foxminded.university.service.appuser.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ua.foxminded.university.service.util.validation.config.ValidatorConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class AppUserCreateDtoValidatorTest {

    private static final String EMAIL_OK  = "user@example.com";
    private static final String EMAIL_BAD = "not-an-email";
    private static final String EMAIL_256 = "a".repeat(251) + "@e.io";

    private static final String PWD_OK         = "Abcd1234!";
    private static final String PWD_SHORT      = "A1!a";
    private static final String PWD_NO_UPPER   = "abcd1234!";
    private static final String PWD_NO_LOWER   = "ABCD1234!";
    private static final String PWD_NO_DIGIT   = "Abcdefg!";
    private static final String PWD_NO_SPEC    = "Abcdefg1";
    private static final String PWD_WITH_SPACE = "Abcd 1234!";
    private static final String PWD_101        = "A".repeat(50) + "a".repeat(50) + "!";

    private static final String FIRST_OK = "John";
    private static final String LAST_OK  = "O'Connor";

    private static final String NAME_1      = "J";
    private static final String NAME_DIGIT  = "Ann3";
    private static final String NAME_SYMBOL = "Doe#";
    private static final String NAME_SPACE  = "John Doe";
    private static final String NAME_65     = "A".repeat(65);

    @Autowired
    private Validator validator;

    // ---------- VALID CASES ----------

    @ParameterizedTest(name = "[{index}] valid -> email={0}, pwd={1}, first={2}, last={3}")
    @MethodSource("validCases")
    @DisplayName("AppUserCreateDto: valid payloads produce no violations")
    void validate_validCases_noViolations(String email,
                                          String newPassword,
                                          String firstName,
                                          String lastName) {

        var dto = new AppUserCreateDto(email, newPassword, firstName, lastName);

        Set<ConstraintViolation<AppUserCreateDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
    }

    static Stream<Arguments> validCases() {
        return Stream.of(
                Arguments.of(EMAIL_OK, PWD_OK, FIRST_OK, LAST_OK)
        );
    }

    // ---------- INVALID CASES ----------

    @ParameterizedTest(name = "[{index}] invalid -> email={0}, pwd={1}, first={2}, last={3} (field={4})")
    @MethodSource("invalidCases")
    @DisplayName("AppUserCreateDto: invalid payloads produce violations on expected field")
    void validate_invalidCases_violationsOnExpectedField(String email,
                                                         String newPassword,
                                                         String firstName,
                                                         String lastName,
                                                         String expectedField) {

        var dto = new AppUserCreateDto(email, newPassword, firstName, lastName);

        Set<ConstraintViolation<AppUserCreateDto>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty(), "Expected violations for field " + expectedField + ", but got none");

        var fields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertEquals(1, fields.size(),
                "Expected violations only for field '" + expectedField + "', but got fields: " + fields);
        assertEquals(expectedField, fields.iterator().next(),
                "Violations must be on field '" + expectedField + "'");
    }

    static Stream<Arguments> invalidCases() {
        return Stream.of(
                Arguments.of(null,        PWD_OK, FIRST_OK, LAST_OK, "email"),
                Arguments.of("",          PWD_OK, FIRST_OK, LAST_OK, "email"),
                Arguments.of(EMAIL_BAD,   PWD_OK, FIRST_OK, LAST_OK, "email"),
                Arguments.of(EMAIL_256,   PWD_OK, FIRST_OK, LAST_OK, "email"),

                Arguments.of(EMAIL_OK, null,           FIRST_OK, LAST_OK, "newPassword"),
                Arguments.of(EMAIL_OK, "",             FIRST_OK, LAST_OK, "newPassword"),
                Arguments.of(EMAIL_OK, PWD_SHORT,      FIRST_OK, LAST_OK, "newPassword"),
                Arguments.of(EMAIL_OK, PWD_NO_UPPER,   FIRST_OK, LAST_OK, "newPassword"),
                Arguments.of(EMAIL_OK, PWD_NO_LOWER,   FIRST_OK, LAST_OK, "newPassword"),
                Arguments.of(EMAIL_OK, PWD_NO_DIGIT,   FIRST_OK, LAST_OK, "newPassword"),
                Arguments.of(EMAIL_OK, PWD_NO_SPEC,    FIRST_OK, LAST_OK, "newPassword"),
                Arguments.of(EMAIL_OK, PWD_WITH_SPACE, FIRST_OK, LAST_OK, "newPassword"),
                Arguments.of(EMAIL_OK, PWD_101,        FIRST_OK, LAST_OK, "newPassword"),

                Arguments.of(EMAIL_OK, PWD_OK, null,        LAST_OK, "firstName"),
                Arguments.of(EMAIL_OK, PWD_OK, "",          LAST_OK, "firstName"),
                Arguments.of(EMAIL_OK, PWD_OK, NAME_1,      LAST_OK, "firstName"),
                Arguments.of(EMAIL_OK, PWD_OK, NAME_DIGIT,  LAST_OK, "firstName"),
                Arguments.of(EMAIL_OK, PWD_OK, NAME_SYMBOL, LAST_OK, "firstName"),
                Arguments.of(EMAIL_OK, PWD_OK, NAME_SPACE,  LAST_OK, "firstName"),
                Arguments.of(EMAIL_OK, PWD_OK, NAME_65,     LAST_OK, "firstName"),

                Arguments.of(EMAIL_OK, PWD_OK, FIRST_OK, null,        "lastName"),
                Arguments.of(EMAIL_OK, PWD_OK, FIRST_OK, "",          "lastName"),
                Arguments.of(EMAIL_OK, PWD_OK, FIRST_OK, NAME_1,      "lastName"),
                Arguments.of(EMAIL_OK, PWD_OK, FIRST_OK, NAME_DIGIT,  "lastName"),
                Arguments.of(EMAIL_OK, PWD_OK, FIRST_OK, NAME_SYMBOL, "lastName"),
                Arguments.of(EMAIL_OK, PWD_OK, FIRST_OK, NAME_SPACE,  "lastName"),
                Arguments.of(EMAIL_OK, PWD_OK, FIRST_OK, NAME_65,     "lastName")
        );
    }
}
