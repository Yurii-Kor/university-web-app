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
class AppUserSelfUpdateDtoValidatorTest {

    private static final Long   ID_OK    = 1L;
    private static final String EMAIL_OK = "user@example.com";
    private static final String EMAIL_BAD = "not-an-email";
    private static final String EMAIL_256 = "a".repeat(251) + "@e.io";

    private static final String FIRST_OK = "John";
    private static final String LAST_OK  = "O'Connor";

    private static final String NAME_1      = "J";
    private static final String NAME_DIGIT  = "Ann3";
    private static final String NAME_SYMBOL = "Doe#";
    private static final String NAME_SPACE  = "John Doe";
    private static final String NAME_65     = "A".repeat(65);

    @Autowired
    private Validator validator;

    @ParameterizedTest(name = "[{index}] id={0}, email={1}, first={2}, last={3} -> valid={4}")
    @MethodSource("cases")
    @DisplayName("AppUserSelfUpdateDto: bean validation")
    void validate(Long id,
                  String email,
                  String firstName,
                  String lastName,
                  boolean shouldPass) {

        AppUserSelfUpdateDto dto = new AppUserSelfUpdateDto(id, email, firstName, lastName);
        Set<ConstraintViolation<AppUserSelfUpdateDto>> violations = validator.validate(dto);

        if (shouldPass) {
            assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
        } else {
            assertFalse(violations.isEmpty(), "Expected violations, but got none");
        }
    }

    static Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of(ID_OK, null,      null,      null,      true),
                Arguments.of(ID_OK, EMAIL_OK,  FIRST_OK,  LAST_OK,   true),
                Arguments.of(ID_OK, "",        null,      null,      true),

                Arguments.of(null, EMAIL_OK, FIRST_OK, LAST_OK, false),

                Arguments.of(ID_OK, EMAIL_BAD,  FIRST_OK, LAST_OK, false),
                Arguments.of(ID_OK, EMAIL_256,  FIRST_OK, LAST_OK, false),

                Arguments.of(ID_OK, EMAIL_OK, NAME_1,      LAST_OK, false),
                Arguments.of(ID_OK, EMAIL_OK, NAME_DIGIT,  LAST_OK, false),
                Arguments.of(ID_OK, EMAIL_OK, NAME_SYMBOL, LAST_OK, false),
                Arguments.of(ID_OK, EMAIL_OK, NAME_SPACE,  LAST_OK, false),
                Arguments.of(ID_OK, EMAIL_OK, NAME_65,     LAST_OK, false),

                Arguments.of(ID_OK, EMAIL_OK, FIRST_OK, NAME_1,      false),
                Arguments.of(ID_OK, EMAIL_OK, FIRST_OK, NAME_DIGIT,  false),
                Arguments.of(ID_OK, EMAIL_OK, FIRST_OK, NAME_SYMBOL, false),
                Arguments.of(ID_OK, EMAIL_OK, FIRST_OK, NAME_SPACE,  false),
                Arguments.of(ID_OK, EMAIL_OK, FIRST_OK, NAME_65,     false)
        );
    }
}
