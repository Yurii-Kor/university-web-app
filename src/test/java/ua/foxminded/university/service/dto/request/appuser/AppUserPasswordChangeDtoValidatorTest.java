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
class AppUserPasswordChangeDtoValidatorTest {

    private static final Long   ID_OK   = 1L;
    private static final String PWD_OK  = "Abcd1234!";
    private static final String CURR_OK = "whatever";

    private static final String PWD_SHORT      = "A1!a";
    private static final String PWD_NO_UPPER   = "abcd1234!";
    private static final String PWD_NO_LOWER   = "ABCD1234!";
    private static final String PWD_NO_DIGIT   = "Abcdefg!";
    private static final String PWD_NO_SPEC    = "Abcdefg1";
    private static final String PWD_WITH_SPACE = "Abcd 1234!";
    private static final String PWD_101        = "A".repeat(50) + "a".repeat(50) + "!";

    @Autowired
    private Validator validator;

    // ---------- VALID CASES ----------

    @ParameterizedTest(name = "[{index}] valid -> id={0}, currPwd={1}, newPwd={2}")
    @MethodSource("validCases")
    @DisplayName("AppUserPasswordChangeDto: valid payloads produce no violations")
    void validate_validCases_noViolations(Long id,
                                          String currentPassword,
                                          String newPassword) {

        var dto = new AppUserPasswordChangeDto(id, currentPassword, newPassword);

        Set<ConstraintViolation<AppUserPasswordChangeDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
    }

    static Stream<Arguments> validCases() {
        return Stream.of(
                Arguments.of(ID_OK, CURR_OK, PWD_OK)
        );
    }

    // ---------- INVALID CASES ----------

    @ParameterizedTest(name = "[{index}] invalid -> id={0}, currPwd={1}, newPwd={2}, field={3}")
    @MethodSource("invalidCases")
    @DisplayName("AppUserPasswordChangeDto: invalid payloads produce violations on expected field")
    void validate_invalidCases_violationsOnExpectedField(Long id,
                                                         String currentPassword,
                                                         String newPassword,
                                                         String expectedField) {

        var dto = new AppUserPasswordChangeDto(id, currentPassword, newPassword);

        Set<ConstraintViolation<AppUserPasswordChangeDto>> violations = validator.validate(dto);

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
                Arguments.of(null, CURR_OK, PWD_OK, "id"),

                Arguments.of(ID_OK, null,   PWD_OK, "currentPassword"),
                Arguments.of(ID_OK, "",     PWD_OK, "currentPassword"),
                Arguments.of(ID_OK, "   ",  PWD_OK, "currentPassword"),

                Arguments.of(ID_OK, CURR_OK, null,           "newPassword"),
                Arguments.of(ID_OK, CURR_OK, "",             "newPassword"),
                Arguments.of(ID_OK, CURR_OK, PWD_SHORT,      "newPassword"),
                Arguments.of(ID_OK, CURR_OK, PWD_NO_UPPER,   "newPassword"),
                Arguments.of(ID_OK, CURR_OK, PWD_NO_LOWER,   "newPassword"),
                Arguments.of(ID_OK, CURR_OK, PWD_NO_DIGIT,   "newPassword"),
                Arguments.of(ID_OK, CURR_OK, PWD_NO_SPEC,    "newPassword"),
                Arguments.of(ID_OK, CURR_OK, PWD_WITH_SPACE, "newPassword"),
                Arguments.of(ID_OK, CURR_OK, PWD_101,        "newPassword")
        );
    }
}
