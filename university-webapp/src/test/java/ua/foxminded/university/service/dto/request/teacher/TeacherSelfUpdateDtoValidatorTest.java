package ua.foxminded.university.service.dto.request.teacher;

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

import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class TeacherSelfUpdateDtoValidatorTest {

    private static final String CASE_VALID   = "valid -> id={0}, rank={1}, office={2}";
    private static final String CASE_INVALID = "invalid -> id={0}, rank={1}, office={2}, field={3}";

    private static final Long   VALID_ID     = 123L;
    private static final String VALID_OFFICE = "B-105";

    private static final String OFFICE_LOWER_LETTER = "b-105";
    private static final String OFFICE_NO_DASH      = "B105";
    private static final String OFFICE_TWO_DIGITS   = "B-12";
    private static final String OFFICE_FOUR_DIGITS  = "B-1000";
    private static final String OFFICE_EMPTY        = "";

    private static final AcademicRank SOME_RANK = AcademicRank.LECTURER;

    @Autowired
    private Validator validator;

    // ========== VALID CASES ==========

    @ParameterizedTest(name = CASE_VALID)
    @MethodSource("validCases")
    @DisplayName("TeacherSelfUpdateDto: valid payloads produce no violations")
    void validate_validCases_noViolations(Long id,
                                          AcademicRank rank,
                                          String office) {

        TeacherSelfUpdateDto dto = new TeacherSelfUpdateDto(id, rank, office);
        Set<ConstraintViolation<TeacherSelfUpdateDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
    }

    static Stream<Arguments> validCases() {
        return Stream.of(
                Arguments.of(VALID_ID, null,        null),
                Arguments.of(VALID_ID, null,        VALID_OFFICE),
                Arguments.of(VALID_ID, SOME_RANK,   VALID_OFFICE)
        );
    }

    // ========== INVALID CASES ==========

    @ParameterizedTest(name = CASE_INVALID)
    @MethodSource("invalidCases")
    @DisplayName("TeacherSelfUpdateDto: invalid payloads produce violations on expected field")
    void validate_invalidCases_violationOnExpectedField(Long id,
                                                        AcademicRank rank,
                                                        String office,
                                                        String expectedField) {

        TeacherSelfUpdateDto dto = new TeacherSelfUpdateDto(id, rank, office);
        Set<ConstraintViolation<TeacherSelfUpdateDto>> violations = validator.validate(dto);

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
                Arguments.of(null,     null,        VALID_OFFICE,      "id"),

                Arguments.of(VALID_ID, null,        OFFICE_EMPTY,       "office"),
                Arguments.of(VALID_ID, null,        OFFICE_LOWER_LETTER,"office"),
                Arguments.of(VALID_ID, null,        OFFICE_NO_DASH,     "office"),
                Arguments.of(VALID_ID, null,        OFFICE_TWO_DIGITS,  "office"),
                Arguments.of(VALID_ID, null,        OFFICE_FOUR_DIGITS, "office")
        );
    }
}
