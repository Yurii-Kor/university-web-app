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
import ua.foxminded.university.service.util.validation.groups.OnChangePassword;
import ua.foxminded.university.service.util.validation.groups.OnCreate;
import ua.foxminded.university.service.util.validation.groups.OnUpdateSelf;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class AppUserDtoValidatorTest {

    private static final Long   ID_OK          = 1L;
    private static final String EMAIL_OK       = "user@example.com";
    private static final String PWD_OK         = "Abcd1234!";  
    private static final String CURR_OK        = "whatever"; 
    private static final String FIRST_OK       = "John";
    private static final String LAST_OK        = "O'Connor";

    private static final String EMAIL_BAD      = "not-an-email";
    private static final String EMAIL_256      = "a".repeat(251) + "@e.io";

    private static final String PWD_SHORT      = "A1!a";
    private static final String PWD_NO_UPPER   = "abcd1234!";
    private static final String PWD_NO_LOWER   = "ABCD1234!";
    private static final String PWD_NO_DIGIT   = "Abcdefg!";
    private static final String PWD_NO_SPEC    = "Abcdefg1";
    private static final String PWD_WITH_SPACE = "Abcd 1234!";
    private static final String PWD_101        = "A".repeat(50) + "a".repeat(50) + "!";

    private static final String NAME_1         = "J";
    private static final String NAME_DIGIT     = "Ann3";
    private static final String NAME_SYMBOL    = "Doe#";
    private static final String NAME_SPACE     = "John Doe";
    private static final String NAME_65        = "A".repeat(65);

    @Autowired
    private Validator validator;

    @ParameterizedTest(name = "[{index}] group={0}, id={1}, email={2}, newPwd={3}, currPwd={4}, first={5}, last={6} -> valid={7}")
    @MethodSource("cases")
    @DisplayName("AppUserDto: bean validation across groups (OnCreate / OnUpdateSelf / OnChangePassword)")
    void validate(Class<?> group,
                  Long id,
                  String email,
                  String newPassword,
                  String currentPassword,
                  String firstName,
                  String lastName,
                  boolean shouldPass) {

        AppUserDto dto = new AppUserDto(id, email, newPassword, currentPassword, firstName, lastName);
        Set<ConstraintViolation<AppUserDto>> violations = validator.validate(dto, group);

        if (shouldPass) {
            assertTrue(violations.isEmpty(), "Expected no violations, but got: " + violations);
        } else {
            assertFalse(violations.isEmpty(), "Expected violations, but got none");
        }
    }

    static Stream<Arguments> cases() {
        return Stream.of(
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_OK, null, FIRST_OK, LAST_OK, true),

            Arguments.of(OnCreate.class, null, null,       PWD_OK, null, FIRST_OK, LAST_OK, false),
            Arguments.of(OnCreate.class, null, "",         PWD_OK, null, FIRST_OK, LAST_OK, false),
            Arguments.of(OnCreate.class, null, EMAIL_BAD,  PWD_OK, null, FIRST_OK, LAST_OK, false),
            Arguments.of(OnCreate.class, null, EMAIL_256,  PWD_OK, null, FIRST_OK, LAST_OK, false),

            Arguments.of(OnCreate.class, null, EMAIL_OK, null,          null, FIRST_OK, LAST_OK, false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_SHORT,     null, FIRST_OK, LAST_OK, false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_NO_UPPER,  null, FIRST_OK, LAST_OK, false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_NO_LOWER,  null, FIRST_OK, LAST_OK, false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_NO_DIGIT,  null, FIRST_OK, LAST_OK, false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_NO_SPEC,   null, FIRST_OK, LAST_OK, false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_WITH_SPACE,null, FIRST_OK, LAST_OK, false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_101,       null, FIRST_OK, LAST_OK, false),

            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_OK, null, null,      LAST_OK,  false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_OK, null, "",        LAST_OK,  false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_OK, null, NAME_1,    LAST_OK,  false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_OK, null, NAME_DIGIT,LAST_OK,  false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_OK, null, NAME_SYMBOL,LAST_OK, false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_OK, null, NAME_SPACE,LAST_OK,  false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_OK, null, NAME_65,   LAST_OK,  false),

            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_OK, null, FIRST_OK, null,       false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_OK, null, FIRST_OK, "",         false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_OK, null, FIRST_OK, NAME_1,     false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_OK, null, FIRST_OK, NAME_DIGIT, false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_OK, null, FIRST_OK, NAME_SYMBOL,false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_OK, null, FIRST_OK, NAME_SPACE, false),
            Arguments.of(OnCreate.class, null, EMAIL_OK, PWD_OK, null, FIRST_OK, NAME_65,    false),

            Arguments.of(OnUpdateSelf.class, ID_OK, null,      null, null, null,     null,     true),
            Arguments.of(OnUpdateSelf.class, ID_OK, EMAIL_OK,  null, null, FIRST_OK, LAST_OK,  true),

            Arguments.of(OnUpdateSelf.class, null,  EMAIL_OK,  null, null, FIRST_OK, LAST_OK,  false),

            Arguments.of(OnUpdateSelf.class, ID_OK, EMAIL_BAD, null, null, FIRST_OK, LAST_OK,  false),
            Arguments.of(OnUpdateSelf.class, ID_OK, EMAIL_256, null, null, FIRST_OK, LAST_OK,  false),
            Arguments.of(OnUpdateSelf.class, ID_OK, EMAIL_OK,  null, null, NAME_1,   LAST_OK,  false),
            Arguments.of(OnUpdateSelf.class, ID_OK, EMAIL_OK,  null, null, FIRST_OK, NAME_1,   false),
            Arguments.of(OnUpdateSelf.class, ID_OK, EMAIL_OK,  null, null, NAME_SPACE, LAST_OK,false),
            Arguments.of(OnUpdateSelf.class, ID_OK, EMAIL_OK,  null, null, FIRST_OK, NAME_SYMBOL, false),

            Arguments.of(OnChangePassword.class, ID_OK, null, PWD_OK, CURR_OK, null, null, true),

            Arguments.of(OnChangePassword.class, null, null, PWD_OK, CURR_OK, null, null, false),
            Arguments.of(OnChangePassword.class, ID_OK, null, null,    CURR_OK, null, null, false),
            Arguments.of(OnChangePassword.class, ID_OK, null, PWD_SHORT, CURR_OK, null, null, false),
            Arguments.of(OnChangePassword.class, ID_OK, null, PWD_NO_UPPER, CURR_OK, null, null, false),
            Arguments.of(OnChangePassword.class, ID_OK, null, PWD_NO_LOWER, CURR_OK, null, null, false),
            Arguments.of(OnChangePassword.class, ID_OK, null, PWD_NO_DIGIT, CURR_OK, null, null, false),
            Arguments.of(OnChangePassword.class, ID_OK, null, PWD_NO_SPEC,  CURR_OK, null, null, false),
            Arguments.of(OnChangePassword.class, ID_OK, null, PWD_WITH_SPACE, CURR_OK, null, null, false),
            Arguments.of(OnChangePassword.class, ID_OK, null, PWD_101,    CURR_OK, null, null, false),
            Arguments.of(OnChangePassword.class, ID_OK, null, PWD_OK, null,  null, null, false),
            Arguments.of(OnChangePassword.class, ID_OK, null, PWD_OK, "   ", null, null, false)
        );
    }
}
