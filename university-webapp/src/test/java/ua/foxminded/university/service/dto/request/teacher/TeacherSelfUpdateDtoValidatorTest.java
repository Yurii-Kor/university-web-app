package ua.foxminded.university.service.dto.request.teacher;

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

import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class TeacherSelfUpdateDtoValidatorTest {

    private static final Long VALID_ID = 123L;
    private static final String VALID_OFFICE = "B-105";

    private static final String OFFICE_LOWER_LETTER = "b-105";
    private static final String OFFICE_NO_DASH      = "B105";
    private static final String OFFICE_TWO_DIGITS   = "B-12";
    private static final String OFFICE_FOUR_DIGITS  = "B-1000";
    private static final String OFFICE_EMPTY        = "";

    private static final AcademicRank SOME_RANK = AcademicRank.LECTURER;

    @Autowired
    private Validator validator;

    @ParameterizedTest(name = "[{index}] id={0}, rank={1}, office={2} -> valid={3}")
    @MethodSource("cases")
    @DisplayName("TeacherSelfUpdateDto: bean validation (default group)")
    void validate(Long id,
                  AcademicRank rank,
                  String office,
                  boolean shouldPass) {

        TeacherSelfUpdateDto dto = new TeacherSelfUpdateDto(id, rank, office);
        Set<ConstraintViolation<TeacherSelfUpdateDto>> violations = validator.validate(dto);

        if (shouldPass) {
            assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
        } else {
            assertFalse(violations.isEmpty(), "Expected violations, but got none");
        }
    }

    static Stream<Arguments> cases() {
        return Stream.of(
            Arguments.of(VALID_ID, null, null, true),
            Arguments.of(VALID_ID, null, VALID_OFFICE, true),
            Arguments.of(VALID_ID, SOME_RANK, VALID_OFFICE, true),

            Arguments.of(null,     null,        VALID_OFFICE, false),
            Arguments.of(VALID_ID, null,        OFFICE_EMPTY, false),
            Arguments.of(VALID_ID, null,        OFFICE_LOWER_LETTER, false),
            Arguments.of(VALID_ID, null,        OFFICE_NO_DASH,      false),
            Arguments.of(VALID_ID, null,        OFFICE_TWO_DIGITS,   false),
            Arguments.of(VALID_ID, null,        OFFICE_FOUR_DIGITS,  false)
        );
    }
}
