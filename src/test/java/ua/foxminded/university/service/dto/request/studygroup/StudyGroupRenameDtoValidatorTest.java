package ua.foxminded.university.service.dto.request.studygroup;

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
class StudyGroupRenameDtoValidatorTest {

    private static final String CASE_VALID   = "valid -> id={0}, name=\"{1}\"";
    private static final String CASE_INVALID = "invalid -> id={0}, name=\"{1}\", field={2}";

    private static final String VALID = "CS-101";

    private static final String EMPTY             = "";
    private static final String LOWERCASE         = "cs-101";
    private static final String MISSING_HYPHEN    = "CS101";
    private static final String WRONG_PREFIX_LEN  = "AAA-101";
    private static final String WRONG_DIGITS_SHORT= "CS-10";
    private static final String WRONG_DIGITS_LONG = "CS-1000";
    private static final String MIXED_CASE        = "Cs-101";
    private static final String NON_LETTER        = "C1-101";
    private static final String WITH_SPACE        = "CS- 101";
    private static final String TRAILING_SPACE    = "CS-101 ";

    private static final Long SOME_ID = 123L;

    @Autowired
    private Validator validator;

    // ---------- VALID ----------

    @ParameterizedTest(name = CASE_VALID)
    @MethodSource("validCases")
    @DisplayName("StudyGroupRenameDto: valid payloads produce no violations")
    void validate_validCases_noViolations(Long id, String name) {
        StudyGroupRenameDto dto = new StudyGroupRenameDto(id, name);
        Set<ConstraintViolation<StudyGroupRenameDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
    }

    static Stream<Arguments> validCases() {
        return Stream.of(
                Arguments.of(SOME_ID, VALID)
        );
    }

    // ---------- INVALID ----------

    @ParameterizedTest(name = CASE_INVALID)
    @MethodSource("invalidCases")
    @DisplayName("StudyGroupRenameDto: invalid payloads produce violations on expected field")
    void validate_invalidCases_violationOnExpectedField(Long id, String name, String expectedField) {
        StudyGroupRenameDto dto = new StudyGroupRenameDto(id, name);
        Set<ConstraintViolation<StudyGroupRenameDto>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty(),
                "Expected violations for field '" + expectedField + "', but got none");

        var fields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertEquals(1, fields.size(),
                "Expected only one field to violate constraints ('" + expectedField + "'), but got: " + fields);
        assertEquals(expectedField, fields.iterator().next(),
                "Violations must be on field '" + expectedField + "'");
    }

    static Stream<Arguments> invalidCases() {
        return Stream.of(
                Arguments.of(null,    VALID,            "id"),

                Arguments.of(SOME_ID, null,             "name"),
                Arguments.of(SOME_ID, EMPTY,            "name"),
                Arguments.of(SOME_ID, LOWERCASE,        "name"),
                Arguments.of(SOME_ID, MISSING_HYPHEN,   "name"),
                Arguments.of(SOME_ID, WRONG_PREFIX_LEN, "name"),
                Arguments.of(SOME_ID, WRONG_DIGITS_SHORT, "name"),
                Arguments.of(SOME_ID, WRONG_DIGITS_LONG,  "name"),
                Arguments.of(SOME_ID, MIXED_CASE,       "name"),
                Arguments.of(SOME_ID, NON_LETTER,       "name"),
                Arguments.of(SOME_ID, WITH_SPACE,       "name"),
                Arguments.of(SOME_ID, TRAILING_SPACE,   "name")
        );
    }
}
