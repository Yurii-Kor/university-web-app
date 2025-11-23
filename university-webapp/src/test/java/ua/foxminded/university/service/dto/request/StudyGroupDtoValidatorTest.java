package ua.foxminded.university.service.dto.request;

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
import ua.foxminded.university.service.util.validation.groups.OnCreate;
import ua.foxminded.university.service.util.validation.groups.OnRename;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class StudyGroupDtoValidatorTest {

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

    @ParameterizedTest(name = "[{index}] group={0}, id={1}, name=\"{2}\" -> valid={3}")
    @MethodSource("cases")
    @DisplayName("StudyGroupDto: bean validation across groups (OnCreate / OnRename)")
    void validate(Class<?> group, Long id, String name, boolean shouldPass) {
        StudyGroupDto dto = new StudyGroupDto(id, name);
        Set<ConstraintViolation<StudyGroupDto>> violations = validator.validate(dto, group);

        if (shouldPass) {
            assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
        } else {
            assertFalse(violations.isEmpty(), "Expected violations, but got none");
        }
    }

    static Stream<Arguments> cases() {
        return Stream.of(
            // ---------- OnCreate: valid ----------
            Arguments.of(OnCreate.class, null,  VALID, true),
            Arguments.of(OnCreate.class, 10L,  VALID, true), 

            // ---------- OnCreate: invalid ----------
            Arguments.of(OnCreate.class, null,  null,               false),
            Arguments.of(OnCreate.class, null,  EMPTY,              false),
            Arguments.of(OnCreate.class, null,  LOWERCASE,          false),
            Arguments.of(OnCreate.class, null,  MISSING_HYPHEN,     false),
            Arguments.of(OnCreate.class, null,  WRONG_PREFIX_LEN,   false),
            Arguments.of(OnCreate.class, null,  WRONG_DIGITS_SHORT, false),
            Arguments.of(OnCreate.class, null,  WRONG_DIGITS_LONG,  false),
            Arguments.of(OnCreate.class, null,  MIXED_CASE,         false),
            Arguments.of(OnCreate.class, null,  NON_LETTER,         false),
            Arguments.of(OnCreate.class, null,  WITH_SPACE,         false),
            Arguments.of(OnCreate.class, null,  TRAILING_SPACE,     false),

            // ---------- OnRename: valid ----------
            Arguments.of(OnRename.class, 123L,  VALID, true),

            // ---------- OnRename: invalid ----------
            Arguments.of(OnRename.class, null,  VALID,              false), 
            Arguments.of(OnRename.class, 123L,  null,               false),
            Arguments.of(OnRename.class, 123L,  EMPTY,              false),
            Arguments.of(OnRename.class, 123L,  LOWERCASE,          false),
            Arguments.of(OnRename.class, 123L,  MISSING_HYPHEN,     false),
            Arguments.of(OnRename.class, 123L,  WRONG_PREFIX_LEN,   false),
            Arguments.of(OnRename.class, 123L,  WRONG_DIGITS_SHORT, false),
            Arguments.of(OnRename.class, 123L,  WRONG_DIGITS_LONG,  false),
            Arguments.of(OnRename.class, 123L,  MIXED_CASE,         false),
            Arguments.of(OnRename.class, 123L,  NON_LETTER,         false),
            Arguments.of(OnRename.class, 123L,  WITH_SPACE,         false),
            Arguments.of(OnRename.class, 123L,  TRAILING_SPACE,     false)
        );
    }
}
