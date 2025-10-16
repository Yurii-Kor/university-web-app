package ua.foxminded.university.model.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.domain.validation.RawPassword;
import ua.foxminded.university.model.domain.validation.config.ValidatorConfig;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class })
class AppUserValidatorTest {

	private static final String CASE_PATTERN = "email={0}, pwd={1}, role={2}, firstName={3}, lastName={4}, createdAt={5} | expected valid: {6}";

	private static final String NULL = "__NULL__";

	private static final String VALID_EMAIL = "user@example.com";
	private static final String VALID_PASSWORD = "Abcd1234!";
	private static final String VALID_FIRST_NAME = "John";
	private static final String VALID_LAST_NAME = "O'Connor";
	private static final UserRole VALID_ROLE = UserRole.STUDENT;

	private static final String BAD_EMAIL = "not-an-email";
	private static final String SHORT_PWD = "A1!a";
	private static final String PWD_NO_UPPER = "abcd1234!";
	private static final String PWD_NO_LOWER = "ABCD1234!";
	private static final String PWD_NO_DIGIT = "Abcdefg!";
	private static final String PWD_NO_SPECIAL = "Abcdefg1";
	private static final String PWD_WITH_SPACE = "Abcd 1234!";
	private static final String NAME_TOO_SHORT = "J";
	private static final String NAME_WITH_DIGIT = "Ann3";
	private static final String NAME_WITH_SYMBOL = "Doe#";
	private static final String NAME_WITH_SPACE = "John Doe";

	@Autowired
	private Validator validator;

	@ParameterizedTest(name = CASE_PATTERN)
	@MethodSource("cases")
	@DisplayName("AppUser bean validation should behave as expected")
	void validateAppUser_ShouldBehaveAsExpected(String email, String password, UserRole role, String firstName,
			String lastName, OffsetDateTime createdAt, boolean shouldPass) {

		String e = NULL.equals(email) ? null : email;
		String p = NULL.equals(password) ? null : password;
		UserRole r = role;
		String fn = NULL.equals(firstName) ? null : firstName;
		String ln = NULL.equals(lastName) ? null : lastName;

		AppUser user = AppUser.builder()
				.id(null)
				.email(e)
				.password(p)
				.role(r)
				.firstName(fn)
				.lastName(ln)
				.enabled(true)
				.createdAt(createdAt)
				.build();

		Set<ConstraintViolation<AppUser>> beanViolations = validator.validate(user);
		
		Set<ConstraintViolation<AppUser>> pwdViolations = (p == null)
                		? Set.of()
                		: validator.validateValue(AppUser.class, "password", p, RawPassword.class);

        Set<ConstraintViolation<?>> allViolations = Stream.concat(
                        beanViolations.stream().map(v -> (ConstraintViolation<?>) v),
                        pwdViolations.stream().map(v -> (ConstraintViolation<?>) v))
						.collect(Collectors.toCollection(HashSet::new));

		if (shouldPass) {
			assertTrue(allViolations.isEmpty(), "Expected no violations, but got: " + allViolations);
		} else {
			assertFalse(allViolations.isEmpty(), "Expected violations, but got none");
		}
	}

	static Stream<Arguments> cases() {
		OffsetDateTime now = OffsetDateTime.now();

		return Stream.of(
				Arguments.of(VALID_EMAIL, VALID_PASSWORD, VALID_ROLE, VALID_FIRST_NAME, VALID_LAST_NAME, now, true),

				Arguments.of(NULL, VALID_PASSWORD, VALID_ROLE, VALID_FIRST_NAME, VALID_LAST_NAME, now, false),
				Arguments.of(BAD_EMAIL, VALID_PASSWORD, VALID_ROLE, VALID_FIRST_NAME, VALID_LAST_NAME, now, false),

				Arguments.of(VALID_EMAIL, SHORT_PWD, VALID_ROLE, VALID_FIRST_NAME, VALID_LAST_NAME, now, false),
				Arguments.of(VALID_EMAIL, PWD_NO_UPPER, VALID_ROLE, VALID_FIRST_NAME, VALID_LAST_NAME, now, false),
				Arguments.of(VALID_EMAIL, PWD_NO_LOWER, VALID_ROLE, VALID_FIRST_NAME, VALID_LAST_NAME, now, false),
				Arguments.of(VALID_EMAIL, PWD_NO_DIGIT, VALID_ROLE, VALID_FIRST_NAME, VALID_LAST_NAME, now, false),
				Arguments.of(VALID_EMAIL, PWD_NO_SPECIAL, VALID_ROLE, VALID_FIRST_NAME, VALID_LAST_NAME, now, false),
				Arguments.of(VALID_EMAIL, PWD_WITH_SPACE, VALID_ROLE, VALID_FIRST_NAME, VALID_LAST_NAME, now, false),

				Arguments.of(VALID_EMAIL, VALID_PASSWORD, null, VALID_FIRST_NAME, VALID_LAST_NAME, now, false),

				Arguments.of(VALID_EMAIL, VALID_PASSWORD, VALID_ROLE, NULL, VALID_LAST_NAME, now, false),
				Arguments.of(VALID_EMAIL, VALID_PASSWORD, VALID_ROLE, VALID_FIRST_NAME, NULL, now, false),
				Arguments.of(VALID_EMAIL, VALID_PASSWORD, VALID_ROLE, NAME_TOO_SHORT, VALID_LAST_NAME, now, false),
				Arguments.of(VALID_EMAIL, VALID_PASSWORD, VALID_ROLE, VALID_FIRST_NAME, NAME_TOO_SHORT, now, false),
				Arguments.of(VALID_EMAIL, VALID_PASSWORD, VALID_ROLE, NAME_WITH_DIGIT, VALID_LAST_NAME, now, false),
				Arguments.of(VALID_EMAIL, VALID_PASSWORD, VALID_ROLE, VALID_FIRST_NAME, NAME_WITH_SYMBOL, now, false),
				Arguments.of(VALID_EMAIL, VALID_PASSWORD, VALID_ROLE, NAME_WITH_SPACE, VALID_LAST_NAME, now, false));
	}
}
