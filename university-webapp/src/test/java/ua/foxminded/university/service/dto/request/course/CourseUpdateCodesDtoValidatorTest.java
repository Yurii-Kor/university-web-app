package ua.foxminded.university.service.dto.request.course;

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
class CourseUpdateCodesDtoValidatorTest {

    private static final Long VALID_ID = 123L;

    private static final String VALID_CODE_LONG  = "CSE-ALG-101";
    private static final String VALID_CODE_SHORT = "SEC-303";

    private static final String LOWER        = "cse-alg-101";
    private static final String BAD_CODE     = "bad";
    private static final String BAD_PREFIX   = "C-ALG-101";
    private static final String BAD_MIDLONG  = "CSE-ALGORITHMS-101";
    private static final String BAD_NUM_2DIG = "CSE-ALG-10";
    private static final String BAD_NUM_4DIG = "CSE-ALG-1000";

    @Autowired
    private Validator validator;

    // ---------- VALID ----------

    @ParameterizedTest(name = "[{index}] valid -> id={0}, code={1}")
    @MethodSource("validCases")
    @DisplayName("CourseUpdateCodesDto: valid payloads produce no violations")
    void validate_validCases_noViolations(Long id, String code) {
        var dto = new CourseUpdateCodesDto(id, code);

        Set<ConstraintViolation<CourseUpdateCodesDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
    }

    static Stream<Arguments> validCases() {
        return Stream.of(
                Arguments.of(VALID_ID, VALID_CODE_LONG),
                Arguments.of(VALID_ID, VALID_CODE_SHORT)
        );
    }

    // ---------- INVALID ----------

    @ParameterizedTest(name = "[{index}] invalid -> id={0}, code={1}, field={2}")
    @MethodSource("invalidCases")
    @DisplayName("CourseUpdateCodesDto: invalid payloads produce violations on expected field")
    void validate_invalidCases_violationOnExpectedField(Long id,
                                                        String code,
                                                        String expectedField) {

        var dto = new CourseUpdateCodesDto(id, code);

        Set<ConstraintViolation<CourseUpdateCodesDto>> violations = validator.validate(dto);

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
                Arguments.of(null,     VALID_CODE_LONG,  "id"),

                Arguments.of(VALID_ID, null,         "code"),
                Arguments.of(VALID_ID, "",           "code"),
                Arguments.of(VALID_ID, LOWER,        "code"),
                Arguments.of(VALID_ID, BAD_CODE,     "code"),
                Arguments.of(VALID_ID, BAD_PREFIX,   "code"),
                Arguments.of(VALID_ID, BAD_MIDLONG,  "code"),
                Arguments.of(VALID_ID, BAD_NUM_2DIG, "code"),
                Arguments.of(VALID_ID, BAD_NUM_4DIG, "code")
        );
    }
}
