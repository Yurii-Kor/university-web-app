package ua.foxminded.university.security;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.validation.EntityValidatior;
import ua.foxminded.university.model.domain.validation.config.ValidatorConfig;
import ua.foxminded.university.security.config.PasswordEncoderConfig;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { ValidatorConfig.class, PasswordEncoderConfig.class, PasswordPolicy.class,
		EntityValidatior.class })
class PasswordPolicyTest {

	private static final String VALID_PWD = "Abcd1234!";
	private static final String INVALID_PWD = "A1!a";

	@Autowired
	private PasswordPolicy policy;

	@Autowired
	private PasswordEncoder encoder;

	@Test
	@DisplayName("encodeNewPassword: encodes raw password in-place")
	void encodeNewPassword_encodesInPlace() {
		var user = AppUser.builder().password(VALID_PWD).build();

		policy.encodeNewPassword(user);

		assertNotNull(user.getPassword(), "Encoded password must be set");
		assertNotEquals(VALID_PWD, user.getPassword(), "Raw password must not be stored");
		assertTrue(user.getPassword().startsWith("$2"), "BCrypt hash must start with $2");
		assertTrue(encoder.matches(VALID_PWD, user.getPassword()), "Encoded must match the raw password");
	}

	@Test
	@DisplayName("encodeForChange: returns encoded string matching raw")
	void encodeForChange_returnsEncoded() {
		var encoded = policy.encodeForChange(VALID_PWD);

		assertNotNull(encoded, "Encoded must not be null");
		assertTrue(encoded.startsWith("$2"), "BCrypt hash must start with $2");
		assertTrue(encoder.matches(VALID_PWD, encoded), "Encoded must match the raw password");
	}

	@Test
	@DisplayName("encodeNewPassword: invalid raw password -> ConstraintViolationException")
	void encodeNewPassword_invalid_throws() {
		var user = AppUser.builder().password(INVALID_PWD).build();

		assertThrows(ConstraintViolationException.class,
				() -> policy.encodeNewPassword(user),
				"Invalid raw password must cause ConstraintViolationException");
	}

	@Test
	@DisplayName("encodeForChange: invalid raw password -> ConstraintViolationException")
	void encodeForChange_invalid_throws() {
		assertThrows(ConstraintViolationException.class,
				() -> policy.encodeForChange(INVALID_PWD),
				"Invalid raw password must cause ConstraintViolationException");
	}

	@Test
	@DisplayName("encodeNewPassword: already encoded -> no-op (no re-encode)")
	void encodeNewPassword_alreadyEncoded_noop() {
		var encoded = encoder.encode(VALID_PWD);
		var user = AppUser.builder().password(encoded).build();

		assertDoesNotThrow(() -> policy.encodeNewPassword(user), "Already-encoded password must not cause errors");

		assertEquals(encoded, user.getPassword(), "Password must not be re-encoded (value should stay the same)");
		assertTrue(encoder.matches(VALID_PWD, user.getPassword()),
				"Encoded must still match the original raw password");
	}

	@Test
	@DisplayName("assertCurrentMatches: raw matches encoded -> does not throw")
	void assertCurrentMatches_valid_ok() {
		var encoded = encoder.encode(VALID_PWD);
		assertDoesNotThrow(() -> policy.assertCurrentMatches(VALID_PWD, encoded),
				"Matching raw/encoded must not throw");
	}

	@Test
	@DisplayName("encodeNewPassword: null user -> IllegalArgumentException")
	void encodeNewPassword_nullUser_throws() {
		assertThrows(IllegalArgumentException.class,
				() -> policy.encodeNewPassword(null),
				"Null user must raise IllegalArgumentException");
	}

	@Test
	@DisplayName("encodeNewPassword: null password -> IllegalArgumentException")
	void encodeNewPassword_nullPassword_throws() {
		var user = AppUser.builder().password(null).build();

		assertThrows(IllegalArgumentException.class,
				() -> policy.encodeNewPassword(user),
				"Null password must raise IllegalArgumentException");
	}

	@Test
	@DisplayName("encodeForChange: null raw -> IllegalArgumentException")
	void encodeForChange_nullRaw_throws() {
		assertThrows(IllegalArgumentException.class,
				() -> policy.encodeForChange(null),
				"Null newRaw must raise IllegalArgumentException");
	}

	@Test
	@DisplayName("assertCurrentMatches: raw does NOT match encoded -> IllegalArgumentException")
	void assertCurrentMatches_mismatch_throws() {
		var encoded = encoder.encode(VALID_PWD);
		assertThrows(IllegalArgumentException.class,
				() -> policy.assertCurrentMatches("Wrong123!", encoded),
				"Mismatch must raise IllegalArgumentException");
	}

	@Test
	@DisplayName("assertCurrentMatches: null providedRaw -> IllegalArgumentException")
	void assertCurrentMatches_nullProvidedRaw_throws() {
		var encoded = encoder.encode(VALID_PWD);
		assertThrows(IllegalArgumentException.class,
				() -> policy.assertCurrentMatches(null, encoded),
				"Null providedRaw must raise IllegalArgumentException");
	}

	@Test
	@DisplayName("assertCurrentMatches: null storedEncoded -> IllegalArgumentException")
	void assertCurrentMatches_nullStoredEncoded_throws() {
		assertThrows(IllegalArgumentException.class,
				() -> policy.assertCurrentMatches(VALID_PWD, null),
				"Null storedEncoded must raise IllegalArgumentException");
	}

}
