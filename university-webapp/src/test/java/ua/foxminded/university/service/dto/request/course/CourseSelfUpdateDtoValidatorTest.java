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
class CourseSelfUpdateDtoValidatorTest {

    private static final Long VALID_ID = 123L;

    private static final String NAME_OK             = "Operating Systems (Linux)";
    private static final String NAME_EMPTY          = "";
    private static final String NAME_BLANK          = "   ";
    private static final String NAME_BAD_LEAD_SPACE = " security";
    private static final String NAME_255            = "S".repeat(255);
    private static final String NAME_256            = "S".repeat(256);

    private static final String DESC_NULL     = null;
    private static final String DESC_EMPTY    = "";
    private static final String DESC_OK_10K   = "A".repeat(10_000);
    private static final String DESC_TOO_LONG = "A".repeat(10_001);

    @Autowired
    private Validator validator;

    // ---------- VALID ----------

    @ParameterizedTest(name = "[{index}] valid -> id={0}, name=\"{1}\", descLen={2}")
    @MethodSource("validCases")
    @DisplayName("CourseSelfUpdateDto: valid payloads produce no violations")
    void validate_validCases_noViolations(Long id, String name, String description) {
        var dto = new CourseSelfUpdateDto(id, name, description);

        Set<ConstraintViolation<CourseSelfUpdateDto>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
    }

    static Stream<Arguments> validCases() {
        return Stream.of(
                Arguments.of(VALID_ID, null,       DESC_NULL),
                Arguments.of(VALID_ID, NAME_EMPTY, DESC_EMPTY),
                Arguments.of(VALID_ID, NAME_BLANK, DESC_OK_10K),
                Arguments.of(VALID_ID, NAME_OK,  DESC_NULL),
                Arguments.of(VALID_ID, NAME_255, DESC_EMPTY)
        );
    }

    // ---------- INVALID ----------

    @ParameterizedTest(name = "[{index}] invalid -> id={0}, name=\"{1}\", descLen={2}, field={3}")
    @MethodSource("invalidCases")
    @DisplayName("CourseSelfUpdateDto: invalid payloads produce violations on expected field")
    void validate_invalidCases_violationOnExpectedField(Long id,
                                                        String name,
                                                        String description,
                                                        String expectedField) {

        var dto = new CourseSelfUpdateDto(id, name, description);

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
                Arguments.of(null,     NAME_OK,  DESC_NULL,     "id"),

                Arguments.of(VALID_ID, NAME_BAD_LEAD_SPACE, DESC_NULL, "name"),
                Arguments.of(VALID_ID, NAME_256,            DESC_NULL, "name"),

                Arguments.of(VALID_ID, NAME_OK, DESC_TOO_LONG, "description")
        );
    }
}
