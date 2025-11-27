package ua.foxminded.university.service.dto.request.course;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
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

    private static final String NAME_OK              = "Operating Systems (Linux)";
    private static final String NAME_EMPTY           = "";
    private static final String NAME_BLANK           = "   ";
    private static final String NAME_BAD_LEAD_SPACE  = " security";
    private static final String NAME_255             = "S".repeat(255);
    private static final String NAME_256             = "S".repeat(256);

    private static final String DESC_NULL     = null;
    private static final String DESC_EMPTY    = "";
    private static final String DESC_OK_10K   = "A".repeat(10_000);
    private static final String DESC_TOO_LONG = "A".repeat(10_001);

    @Autowired
    private Validator validator;

    @ParameterizedTest(name = "[{index}] id={0}, nameLen={1}, descLen={2} -> valid={3}")
    @MethodSource("cases")
    @DisplayName("CourseSelfUpdateDto: bean validation")
    void validate(Long id, String name, String description, boolean shouldPass) {
        CourseSelfUpdateDto dto = new CourseSelfUpdateDto(id, name, description);
        Set<ConstraintViolation<CourseSelfUpdateDto>> violations = validator.validate(dto);

        if (shouldPass) {
            assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
        } else {
            assertFalse(violations.isEmpty(), "Expected violations, but got none");
        }
    }

    static Stream<Arguments> cases() {
        return Stream.of(
            Arguments.of(VALID_ID, null,        DESC_NULL,    true),
            Arguments.of(VALID_ID, NAME_EMPTY,  DESC_EMPTY,   true),
            Arguments.of(VALID_ID, NAME_BLANK,  DESC_OK_10K,  true),
            Arguments.of(VALID_ID, NAME_OK,     DESC_NULL,    true),
            Arguments.of(VALID_ID, NAME_255,    DESC_EMPTY,   true),

            Arguments.of(null,     NAME_OK,             DESC_NULL,     false), 
            Arguments.of(VALID_ID, NAME_BAD_LEAD_SPACE, DESC_NULL,     false),
            Arguments.of(VALID_ID, NAME_256,            DESC_NULL,     false),
            Arguments.of(VALID_ID, NAME_OK,             DESC_TOO_LONG, false)
        );
    }
}
