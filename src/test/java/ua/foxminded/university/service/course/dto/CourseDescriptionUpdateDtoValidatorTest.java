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
class CourseDescriptionUpdateDtoValidatorTest {

    private static final Long VALID_ID = 123L;

    // ---- DESCRIPTION ----
    private static final String DESC_NULL      = null;
    private static final String DESC_EMPTY     = "";
    private static final String DESC_BLANK     = "   ";
    private static final String DESC_OK        = "Some description";
    private static final String DESC_OK_10K    = "A".repeat(10_000);
    private static final String DESC_TOO_LONG  = "A".repeat(10_001);

    @Autowired
    private Validator validator;

    // ---------- VALID ----------

    @ParameterizedTest(name = "[{index}] valid -> id={0}, descLen={1}")
    @MethodSource("validCases")
    @DisplayName("CourseDescriptionUpdateDto: valid payloads produce no violations")
    void validate_validCases_noViolations(Long id, String description) {
        var dto = new CourseDescriptionUpdateDto(id, description);

        Set<ConstraintViolation<CourseDescriptionUpdateDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
    }

    static Stream<Arguments> validCases() {
        return Stream.of(
                Arguments.of(VALID_ID, DESC_NULL),
                Arguments.of(VALID_ID, DESC_EMPTY),
                Arguments.of(VALID_ID, DESC_BLANK),
                Arguments.of(VALID_ID, DESC_OK),
                Arguments.of(VALID_ID, DESC_OK_10K)
        );
    }

    // ---------- INVALID ----------

    @ParameterizedTest(name = "[{index}] invalid -> id={0}, descLen={1}, field={2}")
    @MethodSource("invalidCases")
    @DisplayName("CourseDescriptionUpdateDto: invalid payloads produce violations on expected field")
    void validate_invalidCases_violationOnExpectedField(Long id,
                                                        String description,
                                                        String expectedField) {

        var dto = new CourseDescriptionUpdateDto(id, description);

        Set<ConstraintViolation<CourseDescriptionUpdateDto>> violations = validator.validate(dto);

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
                Arguments.of(null,     DESC_OK,       "id"),
                Arguments.of(VALID_ID, DESC_TOO_LONG, "description")
        );
    }
}
