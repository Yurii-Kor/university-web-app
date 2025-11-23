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

import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.service.util.validation.config.ValidatorConfig;
import ua.foxminded.university.service.util.validation.groups.OnCreate;
import ua.foxminded.university.service.util.validation.groups.OnUpdateSelf;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class TeacherDtoValidatorTest {

    // ---- Valid baseline ----
    private static final Long VALID_ID = 123L;

    private static final String VALID_EMAIL = "teacher.valid@example.com";
    private static final String VALID_PASSWORD = "Abcd1234!";
    private static final String VALID_FIRST = "Alice";
    private static final String VALID_LAST = "O'Connor";
    private static final AcademicRank VALID_RANK = AcademicRank.LECTURER; 
    private static final String VALID_OFFICE = "B-105";

    // ---- Email variants ----
    private static final String BAD_EMAIL = "not-an-email";
    // 256 total chars: 250 + "@e.com"(6) = 256
    private static final String EMAIL_256 = "a".repeat(250) + "@e.com";
    private static final String EMAIL_EMPTY = "";

    // ---- Password variants ----
    private static final String SHORT_PWD       = "A1!a";
    private static final String PWD_NO_UPPER    = "abcd1234!";
    private static final String PWD_NO_LOWER    = "ABCD1234!";
    private static final String PWD_NO_DIGIT    = "Abcdefg!";
    private static final String PWD_NO_SPECIAL  = "Abcdefg1";
    private static final String PWD_WITH_SPACE  = "Abcd 1234!";
    private static final String PWD_101         = "A".repeat(50) + "a".repeat(50) + "!"; // 101+

    // ---- Name variants ----
    private static final String NAME_WITH_DIGIT  = "Ann3";
    private static final String NAME_WITH_SPACE  = "John Doe";
    private static final String NAME_WITH_SYMBOL = "Doe#";
    private static final String TOO_LONG_NAME    = "A".repeat(65);

    // ---- Office variants ----
    private static final String OFFICE_LOWER_LETTER = "b-105";
    private static final String OFFICE_NO_DASH      = "B105";
    private static final String OFFICE_TWO_DIGITS   = "B-12";
    private static final String OFFICE_FOUR_DIGITS  = "B-1000";
    private static final String OFFICE_EMPTY        = "";

    @Autowired
    private Validator validator;

    @ParameterizedTest(name = "[{index}] group={0}, id={1}, email={2}, pwd={3}, first={4}, last={5}, rank={6}, office={7} -> valid={8}")
    @MethodSource("cases")
    @DisplayName("TeacherDto: bean validation across groups (OnCreate / OnUpdateSelf)")
    void validate(Class<?> group,
                  Long id,
                  String email,
                  String password,
                  String firstName,
                  String lastName,
                  AcademicRank rank,
                  String office,
                  boolean shouldPass) {

        TeacherDto dto = new TeacherDto(id, email, password, firstName, lastName, rank, office);
        Set<ConstraintViolation<TeacherDto>> violations = validator.validate(dto, group);

        if (shouldPass) {
            assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
        } else {
            assertFalse(violations.isEmpty(), "Expected violations, but got none");
        }
    }

    static Stream<Arguments> cases() {
        return Stream.of(
            // ==================== OnCreate: valid ====================
            Arguments.of(OnCreate.class, null, VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_RANK, VALID_OFFICE, true),

            // ==================== OnCreate: email ====================
            Arguments.of(OnCreate.class, null, null,          VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_RANK, VALID_OFFICE, false),
            Arguments.of(OnCreate.class, null, EMAIL_EMPTY,   VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_RANK, VALID_OFFICE, false),
            Arguments.of(OnCreate.class, null, BAD_EMAIL,     VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_RANK, VALID_OFFICE, false),
            Arguments.of(OnCreate.class, null, EMAIL_256,     VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_RANK, VALID_OFFICE, false),

            // ==================== OnCreate: password =================
            Arguments.of(OnCreate.class, null, VALID_EMAIL, null,          VALID_FIRST, VALID_LAST, VALID_RANK, VALID_OFFICE, false),
            Arguments.of(OnCreate.class, null, VALID_EMAIL, SHORT_PWD,     VALID_FIRST, VALID_LAST, VALID_RANK, VALID_OFFICE, false),
            Arguments.of(OnCreate.class, null, VALID_EMAIL, PWD_NO_UPPER,  VALID_FIRST, VALID_LAST, VALID_RANK, VALID_OFFICE, false),
            Arguments.of(OnCreate.class, null, VALID_EMAIL, PWD_NO_LOWER,  VALID_FIRST, VALID_LAST, VALID_RANK, VALID_OFFICE, false),
            Arguments.of(OnCreate.class, null, VALID_EMAIL, PWD_NO_DIGIT,  VALID_FIRST, VALID_LAST, VALID_RANK, VALID_OFFICE, false),
            Arguments.of(OnCreate.class, null, VALID_EMAIL, PWD_NO_SPECIAL,VALID_FIRST, VALID_LAST, VALID_RANK, VALID_OFFICE, false),
            Arguments.of(OnCreate.class, null, VALID_EMAIL, PWD_WITH_SPACE,VALID_FIRST, VALID_LAST, VALID_RANK, VALID_OFFICE, false),
            Arguments.of(OnCreate.class, null, VALID_EMAIL, PWD_101,       VALID_FIRST, VALID_LAST, VALID_RANK, VALID_OFFICE, false),

            // ==================== OnCreate: first/last name ==========
            Arguments.of(OnCreate.class, null, VALID_EMAIL, VALID_PASSWORD, null,            VALID_LAST,  VALID_RANK, VALID_OFFICE, false),
            Arguments.of(OnCreate.class, null, VALID_EMAIL, VALID_PASSWORD, VALID_FIRST,     null,        VALID_RANK, VALID_OFFICE, false),
            Arguments.of(OnCreate.class, null, VALID_EMAIL, VALID_PASSWORD, NAME_WITH_DIGIT, VALID_LAST,  VALID_RANK, VALID_OFFICE, false),
            Arguments.of(OnCreate.class, null, VALID_EMAIL, VALID_PASSWORD, NAME_WITH_SPACE, VALID_LAST,  VALID_RANK, VALID_OFFICE, false),
            Arguments.of(OnCreate.class, null, VALID_EMAIL, VALID_PASSWORD, TOO_LONG_NAME,   VALID_LAST,  VALID_RANK, VALID_OFFICE, false),
            Arguments.of(OnCreate.class, null, VALID_EMAIL, VALID_PASSWORD, VALID_FIRST,     NAME_WITH_SYMBOL, VALID_RANK, VALID_OFFICE, false),

            // ==================== OnCreate: rank =====================
            Arguments.of(OnCreate.class, null, VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, null,        VALID_OFFICE, false),

            // ==================== OnCreate: office ===================
            Arguments.of(OnCreate.class, null, VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_RANK, null,           false),
            Arguments.of(OnCreate.class, null, VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_RANK, OFFICE_EMPTY,   false),
            Arguments.of(OnCreate.class, null, VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_RANK, OFFICE_LOWER_LETTER, false),
            Arguments.of(OnCreate.class, null, VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_RANK, OFFICE_NO_DASH,      false),
            Arguments.of(OnCreate.class, null, VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_RANK, OFFICE_TWO_DIGITS,   false),
            Arguments.of(OnCreate.class, null, VALID_EMAIL, VALID_PASSWORD, VALID_FIRST, VALID_LAST, VALID_RANK, OFFICE_FOUR_DIGITS,  false),

            // ==================== OnUpdateSelf: valid ================
            Arguments.of(OnUpdateSelf.class, VALID_ID, null, null, null, null, null, null,        true),
            Arguments.of(OnUpdateSelf.class, VALID_ID, null, null, null, null, null, VALID_OFFICE, true),

            // ==================== OnUpdateSelf: invalid ==============
            Arguments.of(OnUpdateSelf.class, null,     null, null, null, null, null, VALID_OFFICE, false), 
            Arguments.of(OnUpdateSelf.class, VALID_ID, null, null, null, null, null, OFFICE_EMPTY, false), 
            Arguments.of(OnUpdateSelf.class, VALID_ID, null, null, null, null, null, OFFICE_LOWER_LETTER, false),
            Arguments.of(OnUpdateSelf.class, VALID_ID, null, null, null, null, null, OFFICE_NO_DASH,      false),
            Arguments.of(OnUpdateSelf.class, VALID_ID, null, null, null, null, null, OFFICE_TWO_DIGITS,   false),
            Arguments.of(OnUpdateSelf.class, VALID_ID, null, null, null, null, null, OFFICE_FOUR_DIGITS,  false)
        );
    }
}
