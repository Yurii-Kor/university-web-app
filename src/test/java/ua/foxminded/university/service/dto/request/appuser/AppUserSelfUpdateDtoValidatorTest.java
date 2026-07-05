package ua.foxminded.university.service.dto.request.appuser;

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
class AppUserSelfUpdateDtoValidatorTest {

    private static final Long   ID_OK    = 1L;
    private static final String EMAIL_OK = "user@example.com";
    private static final String EMAIL_BAD = "not-an-email";
    private static final String EMAIL_256 = "a".repeat(251) + "@e.io";

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

    @ParameterizedTest(name = "[{index}] valid -> id={0}, email={1}, first={2}, last={3}")
    @MethodSource("validCases")
    @DisplayName("AppUserSelfUpdateDto: valid payloads produce no violations")
    void validate_validCases_noViolations(Long id,
                                          String email,
                                          String firstName,
                                          String lastName) {

        var dto = new AppUserSelfUpdateDto(id, email, firstName, lastName);
        Set<ConstraintViolation<AppUserSelfUpdateDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
    }

    static Stream<Arguments> validCases() {
        return Stream.of(
                Arguments.of(ID_OK, null,      null,      null),
                Arguments.of(ID_OK, EMAIL_OK,  FIRST_OK,  LAST_OK),
                Arguments.of(ID_OK, "",        null,      null)
        );
    }

    // ---------- INVALID CASES ----------

    @ParameterizedTest(name = "[{index}] invalid -> id={0}, email={1}, first={2}, last={3}, field={4}")
    @MethodSource("invalidCases")
    @DisplayName("AppUserSelfUpdateDto: invalid payloads produce violations on expected field")
    void validate_invalidCases_violationsOnExpectedField(Long id,
                                                         String email,
                                                         String firstName,
                                                         String lastName,
                                                         String expectedField) {

        var dto = new AppUserSelfUpdateDto(id, email, firstName, lastName);
        Set<ConstraintViolation<AppUserSelfUpdateDto>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty(),
                "Expected violations for field " + expectedField + ", but got none");

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
                Arguments.of(null, EMAIL_OK, FIRST_OK, LAST_OK, "id"),

                Arguments.of(ID_OK, EMAIL_BAD,  FIRST_OK, LAST_OK, "email"),
                Arguments.of(ID_OK, EMAIL_256,  FIRST_OK, LAST_OK, "email"),

                Arguments.of(ID_OK, EMAIL_OK, NAME_1,      LAST_OK, "firstName"),
                Arguments.of(ID_OK, EMAIL_OK, NAME_DIGIT,  LAST_OK, "firstName"),
                Arguments.of(ID_OK, EMAIL_OK, NAME_SYMBOL, LAST_OK, "firstName"),
                Arguments.of(ID_OK, EMAIL_OK, NAME_SPACE,  LAST_OK, "firstName"),
                Arguments.of(ID_OK, EMAIL_OK, NAME_65,     LAST_OK, "firstName"),

                Arguments.of(ID_OK, EMAIL_OK, FIRST_OK, NAME_1,      "lastName"),
                Arguments.of(ID_OK, EMAIL_OK, FIRST_OK, NAME_DIGIT,  "lastName"),
                Arguments.of(ID_OK, EMAIL_OK, FIRST_OK, NAME_SYMBOL, "lastName"),
                Arguments.of(ID_OK, EMAIL_OK, FIRST_OK, NAME_SPACE,  "lastName"),
                Arguments.of(ID_OK, EMAIL_OK, FIRST_OK, NAME_65,     "lastName")
        );
    }
}
