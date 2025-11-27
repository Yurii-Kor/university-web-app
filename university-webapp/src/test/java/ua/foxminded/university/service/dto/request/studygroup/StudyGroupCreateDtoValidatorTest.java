package ua.foxminded.university.service.dto.request.studygroup;

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
class StudyGroupCreateDtoValidatorTest {

    private static final String VALID = "CS-101";

    private static final String EMPTY = "";
    private static final String LOWERCASE = "cs-101";
    private static final String MISSING_HYPHEN = "CS101";
    private static final String WRONG_PREFIX_LEN = "AAA-101";
    private static final String WRONG_DIGITS_SHORT = "CS-10";
    private static final String WRONG_DIGITS_LONG = "CS-1000";
    private static final String MIXED_CASE = "Cs-101";
    private static final String NON_LETTER = "C1-101";
    private static final String WITH_SPACE = "CS- 101";
    private static final String TRAILING_SPACE = "CS-101 ";

    @Autowired
    private Validator validator;

    @ParameterizedTest(name = "[{index}] name=\"{0}\" -> valid={1}")
    @MethodSource("cases")
    @DisplayName("StudyGroupCreateDto: bean validation")
    void validate(String name, boolean shouldPass) {
        StudyGroupCreateDto dto = new StudyGroupCreateDto(name);
        Set<ConstraintViolation<StudyGroupCreateDto>> violations = validator.validate(dto);

        if (shouldPass) {
            assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
        } else {
            assertFalse(violations.isEmpty(), "Expected violations, but got none");
        }
    }

    static Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of(VALID, true),

                Arguments.of(null,               false),
                Arguments.of(EMPTY,              false),
                Arguments.of(LOWERCASE,          false),
                Arguments.of(MISSING_HYPHEN,     false),
                Arguments.of(WRONG_PREFIX_LEN,   false),
                Arguments.of(WRONG_DIGITS_SHORT, false),
                Arguments.of(WRONG_DIGITS_LONG,  false),
                Arguments.of(MIXED_CASE,         false),
                Arguments.of(NON_LETTER,         false),
                Arguments.of(WITH_SPACE,         false),
                Arguments.of(TRAILING_SPACE,     false)
        );
    }
}
