package ua.foxminded.university.service.course.dto;

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
class CourseCreateDtoValidatorTest {

    private static final Long TEACHER_ID = 1L;

    private static final String VALID_CODE_LONG  = "CSE-ALG-101";
    private static final String VALID_CODE_SHORT = "SEC-303";

    private static final String LOWER        = "cse-alg-101";
    private static final String BAD_CODE     = "bad";
    private static final String BAD_PREFIX   = "C-ALG-101";
    private static final String BAD_MIDLONG  = "CSE-ALGORITHMS-101";
    private static final String BAD_NUM_2DIG = "CSE-ALG-10";
    private static final String BAD_NUM_4DIG = "CSE-ALG-1000";

    private static final String NAME_OK    = "Operating Systems (Linux)";
    private static final String NAME_EMPTY = "";
    private static final String NAME_BLANK = "   ";
    private static final String NAME_255   = "S".repeat(255);
    private static final String NAME_256   = "S".repeat(256);

    private static final String DESC_NULL     = null;
    private static final String DESC_EMPTY    = "";
    private static final String DESC_OK_10K   = "A".repeat(10_000);
    private static final String DESC_TOO_LONG = "A".repeat(10_001);

    @Autowired
    private Validator validator;

    // ---------- VALID CASES ----------

    @ParameterizedTest(name = "[{index}] valid -> code={0}, nameLen={1}, descLen={2}, teacherId={3}")
    @MethodSource("validCases")
    @DisplayName("CourseCreateDto: valid payloads produce no violations")
    void validate_validCases_noViolations(String code,
                                          String name,
                                          String description,
                                          Long teacherId) {

        var dto = new CourseCreateDto(code, name, description, teacherId);
        Set<ConstraintViolation<CourseCreateDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
    }

    static Stream<Arguments> validCases() {
        return Stream.of(
                Arguments.of(VALID_CODE_LONG,  NAME_OK,  DESC_NULL,    TEACHER_ID),
                Arguments.of(VALID_CODE_SHORT, NAME_OK,  DESC_EMPTY,   TEACHER_ID),
                Arguments.of(VALID_CODE_LONG,  NAME_OK,  DESC_OK_10K,  TEACHER_ID),
                Arguments.of(VALID_CODE_LONG,  NAME_255, DESC_NULL,    TEACHER_ID)
        );
    }

    // ---------- INVALID CASES ----------

    @ParameterizedTest(name = "[{index}] invalid -> code={0}, name={1}, descLen={2}, teacherId={3}, field={4}")
    @MethodSource("invalidCases")
    @DisplayName("CourseCreateDto: invalid payloads produce violations on expected field")
    void validate_invalidCases_violationsOnExpectedField(String code,
                                                         String name,
                                                         String description,
                                                         Long teacherId,
                                                         String expectedField) {

        var dto = new CourseCreateDto(code, name, description, teacherId);
        Set<ConstraintViolation<CourseCreateDto>> violations = validator.validate(dto);

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
                Arguments.of(null,        NAME_OK, DESC_NULL, TEACHER_ID, "code"),
                Arguments.of("",          NAME_OK, DESC_NULL, TEACHER_ID, "code"),
                Arguments.of(LOWER,       NAME_OK, DESC_NULL, TEACHER_ID, "code"),
                Arguments.of(BAD_CODE,    NAME_OK, DESC_NULL, TEACHER_ID, "code"),
                Arguments.of(BAD_PREFIX,  NAME_OK, DESC_NULL, TEACHER_ID, "code"),
                Arguments.of(BAD_MIDLONG, NAME_OK, DESC_NULL, TEACHER_ID, "code"),
                Arguments.of(BAD_NUM_2DIG,BAD_MIDLONG, DESC_NULL, TEACHER_ID, "code"),
                Arguments.of(BAD_NUM_4DIG,NAME_OK, DESC_NULL, TEACHER_ID, "code"),

                Arguments.of(VALID_CODE_LONG, null,        DESC_NULL, TEACHER_ID, "name"),
                Arguments.of(VALID_CODE_LONG, NAME_EMPTY,  DESC_NULL, TEACHER_ID, "name"),
                Arguments.of(VALID_CODE_LONG, NAME_BLANK,  DESC_NULL, TEACHER_ID, "name"),
                Arguments.of(VALID_CODE_LONG, NAME_256,    DESC_NULL, TEACHER_ID, "name"),

                Arguments.of(VALID_CODE_LONG, NAME_OK, DESC_TOO_LONG, TEACHER_ID, "description"),

                Arguments.of(VALID_CODE_LONG, NAME_OK, DESC_NULL, null, "teacherId")
        );
    }
}
