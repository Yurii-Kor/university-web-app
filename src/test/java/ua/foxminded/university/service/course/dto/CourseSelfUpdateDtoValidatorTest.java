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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ua.foxminded.university.service.util.validation.config.ValidatorConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class CourseSelfUpdateDtoValidatorTest {

    private static final Long VALID_ID = 123L;

    // ---- CODE ----
    private static final String CODE_NULL = null;

    // valid by regex:
    private static final String CODE_OK_SIMPLE_MIN = "CS-101";
    private static final String CODE_OK_SIMPLE_MAX_PREFIX = "ABCDE-999";
    private static final String CODE_OK_TWO_SEG_MIN = "CS-AB-001";
    private static final String CODE_OK_TWO_SEG_MAX = "ABCDE-ABCDEFGHI-123";

    // invalid by regex/size:
    private static final String CODE_EMPTY = "";
    private static final String CODE_BLANK = "   ";
    private static final String CODE_BAD_LOWER = "cs-101";
    private static final String CODE_BAD_PREFIX_SHORT = "C-101";
    private static final String CODE_BAD_PREFIX_LONG = "ABCDEF-101";
    private static final String CODE_BAD_DIGITS_2 = "CS-10";
    private static final String CODE_BAD_DIGITS_4 = "CS-1000";
    private static final String CODE_BAD_TWOSEG_MID_SHORT = "CS-A-101";
    private static final String CODE_BAD_TWOSEG_MID_LONG = "CS-ABCDEFGHIJ-101";
    private static final String CODE_BAD_CHAR = "CS_101";
    private static final String CODE_BAD_SPACE_LEAD = " CS-101";
    private static final String CODE_BAD_SPACE_TAIL = "CS-101 ";

    // ---- NAME ----
    private static final String NAME_OK             = "Operating Systems (Linux)";
    private static final String NAME_EMPTY          = "";
    private static final String NAME_BLANK          = "   ";
    private static final String NAME_BAD_LEAD_SPACE = " security";
    private static final String NAME_255            = "S".repeat(255);
    private static final String NAME_256            = "S".repeat(256);

    // ---- TEACHER ID ----
    private static final Long TEACHER_NULL = null;  // optional
    private static final Long TEACHER_OK_1 = 1L;
    private static final Long TEACHER_OK_42 = 42L;
    private static final Long TEACHER_ZERO = 0L;
    private static final Long TEACHER_NEG  = -1L;

    @Autowired
    private Validator validator;

    // ---------- VALID ----------

    @ParameterizedTest(name = "[{index}] valid -> id={0}, code=\"{1}\", name=\"{2}\", teacherId={3}")
    @MethodSource("validCases")
    @DisplayName("CourseSelfUpdateDto: valid payloads produce no violations")
    void validate_validCases_noViolations(Long id, String code, String name, Long teacherId) {
        var dto = new CourseSelfUpdateDto(id, code, name, teacherId);

        Set<ConstraintViolation<CourseSelfUpdateDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
    }

    static Stream<Arguments> validCases() {
        return Stream.of(
                // all optional omitted
                Arguments.of(VALID_ID, CODE_NULL, null, TEACHER_NULL),

                // name-only updates
                Arguments.of(VALID_ID, CODE_NULL, NAME_EMPTY, TEACHER_NULL),
                Arguments.of(VALID_ID, CODE_NULL, NAME_BLANK, TEACHER_NULL),
                Arguments.of(VALID_ID, CODE_NULL, NAME_OK,    TEACHER_NULL),
                Arguments.of(VALID_ID, CODE_NULL, NAME_255,   TEACHER_NULL),

                // code-only updates
                Arguments.of(VALID_ID, CODE_OK_SIMPLE_MIN,         null, TEACHER_NULL),
                Arguments.of(VALID_ID, CODE_OK_SIMPLE_MAX_PREFIX,  null, TEACHER_NULL),
                Arguments.of(VALID_ID, CODE_OK_TWO_SEG_MIN,        null, TEACHER_NULL),
                Arguments.of(VALID_ID, CODE_OK_TWO_SEG_MAX,        null, TEACHER_NULL),

                // teacher-only update
                Arguments.of(VALID_ID, CODE_NULL, null, TEACHER_OK_1),
                Arguments.of(VALID_ID, CODE_NULL, null, TEACHER_OK_42),

                // combinations
                Arguments.of(VALID_ID, CODE_OK_SIMPLE_MIN, NAME_OK,  TEACHER_NULL),
                Arguments.of(VALID_ID, CODE_OK_TWO_SEG_MIN, NAME_255, TEACHER_OK_42)
        );
    }

    // ---------- INVALID ----------

    @ParameterizedTest(name = "[{index}] invalid -> id={0}, code=\"{1}\", name=\"{2}\", teacherId={3}, field={4}")
    @MethodSource("invalidCases")
    @DisplayName("CourseSelfUpdateDto: invalid payloads produce violations on expected field")
    void validate_invalidCases_violationOnExpectedField(Long id,
                                                        String code,
                                                        String name,
                                                        Long teacherId,
                                                        String expectedField) {

        var dto = new CourseSelfUpdateDto(id, code, name, teacherId);

        Set<ConstraintViolation<CourseSelfUpdateDto>> violations = validator.validate(dto);

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
                // id invalid
                Arguments.of(null,     CODE_NULL, NAME_OK, TEACHER_NULL, "id"),

                // code invalid (only code wrong; name ok)
                Arguments.of(VALID_ID, CODE_EMPTY,                NAME_OK, TEACHER_NULL, "code"),
                Arguments.of(VALID_ID, CODE_BLANK,                NAME_OK, TEACHER_NULL, "code"),
                Arguments.of(VALID_ID, CODE_BAD_LOWER,            NAME_OK, TEACHER_NULL, "code"),
                Arguments.of(VALID_ID, CODE_BAD_PREFIX_SHORT,     NAME_OK, TEACHER_NULL, "code"),
                Arguments.of(VALID_ID, CODE_BAD_PREFIX_LONG,      NAME_OK, TEACHER_NULL, "code"),
                Arguments.of(VALID_ID, CODE_BAD_DIGITS_2,         NAME_OK, TEACHER_NULL, "code"),
                Arguments.of(VALID_ID, CODE_BAD_DIGITS_4,         NAME_OK, TEACHER_NULL, "code"),
                Arguments.of(VALID_ID, CODE_BAD_TWOSEG_MID_SHORT, NAME_OK, TEACHER_NULL, "code"),
                Arguments.of(VALID_ID, CODE_BAD_TWOSEG_MID_LONG,  NAME_OK, TEACHER_NULL, "code"),
                Arguments.of(VALID_ID, CODE_BAD_CHAR,             NAME_OK, TEACHER_NULL, "code"),
                Arguments.of(VALID_ID, CODE_BAD_SPACE_LEAD,       NAME_OK, TEACHER_NULL, "code"),
                Arguments.of(VALID_ID, CODE_BAD_SPACE_TAIL,       NAME_OK, TEACHER_NULL, "code"),

                // name invalid (only name wrong; code ok/omitted)
                Arguments.of(VALID_ID, CODE_NULL, NAME_BAD_LEAD_SPACE, TEACHER_NULL, "name"),
                Arguments.of(VALID_ID, CODE_NULL, NAME_256,            TEACHER_NULL, "name"),

                // teacherId invalid (only teacherId wrong; code/name ok/omitted)
                Arguments.of(VALID_ID, CODE_NULL, NAME_OK, TEACHER_ZERO, "teacherId"),
                Arguments.of(VALID_ID, CODE_NULL, NAME_OK, TEACHER_NEG,  "teacherId")
        );
    }
}
