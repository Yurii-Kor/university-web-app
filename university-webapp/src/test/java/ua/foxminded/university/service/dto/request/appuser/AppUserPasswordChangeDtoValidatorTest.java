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
class AppUserPasswordChangeDtoValidatorTest {

    private static final Long   ID_OK   = 1L;
    private static final String PWD_OK  = "Abcd1234!";
    private static final String CURR_OK = "whatever";

    private static final String PWD_SHORT      = "A1!a";
    private static final String PWD_NO_UPPER   = "abcd1234!";
    private static final String PWD_NO_LOWER   = "ABCD1234!";
    private static final String PWD_NO_DIGIT   = "Abcdefg!";
    private static final String PWD_NO_SPEC    = "Abcdefg1";
    private static final String PWD_WITH_SPACE = "Abcd 1234!";
    private static final String PWD_101        = "A".repeat(50) + "a".repeat(50) + "!";

    @Autowired
    private Validator validator;

    @ParameterizedTest(name = "[{index}] id={0}, currPwd={1}, newPwd={2} -> valid={3}")
    @MethodSource("cases")
    @DisplayName("AppUserPasswordChangeDto: bean validation")
    void validate(Long id,
                  String currentPassword,
                  String newPassword,
                  boolean shouldPass) {

        AppUserPasswordChangeDto dto = new AppUserPasswordChangeDto(id, currentPassword, newPassword);
        Set<ConstraintViolation<AppUserPasswordChangeDto>> violations = validator.validate(dto);

        if (shouldPass) {
            assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
        } else {
            assertFalse(violations.isEmpty(), "Expected violations, but got none");
        }
    }

    static Stream<Arguments> cases() {
        return Stream.of(
                Arguments.of(ID_OK, CURR_OK, PWD_OK, true),

                Arguments.of(null, CURR_OK, PWD_OK, false),

                Arguments.of(ID_OK, null,     PWD_OK, false),
                Arguments.of(ID_OK, "",       PWD_OK, false),
                Arguments.of(ID_OK, "   ",    PWD_OK, false),

                Arguments.of(ID_OK, CURR_OK, null,          false),
                Arguments.of(ID_OK, CURR_OK, "",            false),
                Arguments.of(ID_OK, CURR_OK, PWD_SHORT,     false),
                Arguments.of(ID_OK, CURR_OK, PWD_NO_UPPER,  false),
                Arguments.of(ID_OK, CURR_OK, PWD_NO_LOWER,  false),
                Arguments.of(ID_OK, CURR_OK, PWD_NO_DIGIT,  false),
                Arguments.of(ID_OK, CURR_OK, PWD_NO_SPEC,   false),
                Arguments.of(ID_OK, CURR_OK, PWD_WITH_SPACE,false),
                Arguments.of(ID_OK, CURR_OK, PWD_101,       false)
        );
    }
}
