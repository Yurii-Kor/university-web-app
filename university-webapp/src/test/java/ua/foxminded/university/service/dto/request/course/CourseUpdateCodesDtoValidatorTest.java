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

    @ParameterizedTest(name = "[{index}] id={0}, code={1} -> valid={2}")
    @MethodSource("cases")
    @DisplayName("CourseUpdateCodesDto: bean validation")
    void validate(Long id, String code, boolean shouldPass) {
        CourseUpdateCodesDto dto = new CourseUpdateCodesDto(id, code);
        Set<ConstraintViolation<CourseUpdateCodesDto>> violations = validator.validate(dto);

        if (shouldPass) {
            assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
        } else {
            assertFalse(violations.isEmpty(), "Expected violations, but got none");
        }
    }

    static Stream<Arguments> cases() {
        return Stream.of(
            Arguments.of(VALID_ID, VALID_CODE_LONG,  true),
            Arguments.of(VALID_ID, VALID_CODE_SHORT, true),

            Arguments.of(null,     VALID_CODE_LONG,  false),

            Arguments.of(VALID_ID, null,        false),
            Arguments.of(VALID_ID, "",          false),
            Arguments.of(VALID_ID, LOWER,       false),
            Arguments.of(VALID_ID, BAD_CODE,    false),
            Arguments.of(VALID_ID, BAD_PREFIX,  false),
            Arguments.of(VALID_ID, BAD_MIDLONG, false),
            Arguments.of(VALID_ID, BAD_NUM_2DIG,false),
            Arguments.of(VALID_ID, BAD_NUM_4DIG,false)
        );
    }
}
