package ua.foxminded.university.service.dto.request.appuser;

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
class AppUserCreateDtoValidatorTest {

    private static final String EMAIL_OK   = "user@example.com";
    private static final String EMAIL_BAD  = "not-an-email";
    private static final String EMAIL_256  = "a".repeat(251) + "@e.io";

    private static final String PWD_OK         = "Abcd1234!";
    private static final String PWD_SHORT      = "A1!a";
    private static final String PWD_NO_UPPER   = "abcd1234!";
    private static final String PWD_NO_LOWER   = "ABCD1234!";
    private static final String PWD_NO_DIGIT   = "Abcdefg!";
    private static final String PWD_NO_SPEC    = "Abcdefg1";
    private static final String PWD_WITH_SPACE = "Abcd 1234!";
    private static final String PWD_101        = "A".repeat(50) + "a".repeat(50) + "!";

    private static final String FIRST_OK  = "John";
    private static final String LAST_OK   = "O'Connor";

    private static final String NAME_1      = "J";
    private static final String NAME_DIGIT  = "Ann3";
    private static final String NAME_SYMBOL = "Doe#";
    private static final String NAME_SPACE  = "John Doe";
    private static final String NAME_65     = "A".repeat(65);

    @Autowired
    private Validator validator;

    @ParameterizedTest(name = "[{index}] email={0}, newPwd={1}, first={2}, last={3} -> valid={4}")
    @MethodSource("cases")
    @DisplayName("AppUserCreateDto: bean validation")
    void validate(String email,
                  String newPassword,
                  String firstName,
                  String lastName,
                  boolean shouldPass) {

        AppUserCreateDto dto = new AppUserCreateDto(email, newPassword, firstName, lastName);
        Set<ConstraintViolation<AppUserCreateDto>> violations = validator.validate(dto);

        if (shouldPass) {
            assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
        } else {
            assertFalse(violations.isEmpty(), "Expected violations, but got none");
        }
    }

    static Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of(EMAIL_OK, PWD_OK, FIRST_OK, LAST_OK, true),

                Arguments.of(null,        PWD_OK, FIRST_OK, LAST_OK, false), 
                Arguments.of("",          PWD_OK, FIRST_OK, LAST_OK, false),
                Arguments.of(EMAIL_BAD,   PWD_OK, FIRST_OK, LAST_OK, false),
                Arguments.of(EMAIL_256,   PWD_OK, FIRST_OK, LAST_OK, false),

                Arguments.of(EMAIL_OK, null,          FIRST_OK, LAST_OK, false),
                Arguments.of(EMAIL_OK, "",            FIRST_OK, LAST_OK, false),
                Arguments.of(EMAIL_OK, PWD_SHORT,     FIRST_OK, LAST_OK, false),
                Arguments.of(EMAIL_OK, PWD_NO_UPPER,  FIRST_OK, LAST_OK, false),
                Arguments.of(EMAIL_OK, PWD_NO_LOWER,  FIRST_OK, LAST_OK, false),
                Arguments.of(EMAIL_OK, PWD_NO_DIGIT,  FIRST_OK, LAST_OK, false),
                Arguments.of(EMAIL_OK, PWD_NO_SPEC,   FIRST_OK, LAST_OK, false),
                Arguments.of(EMAIL_OK, PWD_WITH_SPACE,FIRST_OK, LAST_OK, false),
                Arguments.of(EMAIL_OK, PWD_101,       FIRST_OK, LAST_OK, false),

                Arguments.of(EMAIL_OK, PWD_OK, null,       LAST_OK, false),
                Arguments.of(EMAIL_OK, PWD_OK, "",         LAST_OK, false),
                Arguments.of(EMAIL_OK, PWD_OK, NAME_1,     LAST_OK, false),  
                Arguments.of(EMAIL_OK, PWD_OK, NAME_DIGIT, LAST_OK, false),
                Arguments.of(EMAIL_OK, PWD_OK, NAME_SYMBOL,LAST_OK, false),
                Arguments.of(EMAIL_OK, PWD_OK, NAME_SPACE, LAST_OK, false),
                Arguments.of(EMAIL_OK, PWD_OK, NAME_65,    LAST_OK, false),

                Arguments.of(EMAIL_OK, PWD_OK, FIRST_OK, null,        false),
                Arguments.of(EMAIL_OK, PWD_OK, FIRST_OK, "",          false),
                Arguments.of(EMAIL_OK, PWD_OK, FIRST_OK, NAME_1,      false),
                Arguments.of(EMAIL_OK, PWD_OK, FIRST_OK, NAME_DIGIT,  false),
                Arguments.of(EMAIL_OK, PWD_OK, FIRST_OK, NAME_SYMBOL, false),
                Arguments.of(EMAIL_OK, PWD_OK, FIRST_OK, NAME_SPACE,  false),
                Arguments.of(EMAIL_OK, PWD_OK, FIRST_OK, NAME_65,     false)
        );
    }
}
