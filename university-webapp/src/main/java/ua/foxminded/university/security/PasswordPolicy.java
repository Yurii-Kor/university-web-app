package ua.foxminded.university.security;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.validation.EntityValidatior;

@Component
@RequiredArgsConstructor
public class PasswordPolicy {

	private static final Logger log = LoggerFactory.getLogger(PasswordPolicy.class);

	private final PasswordEncoder encoder;
	private final EntityValidatior validator;

	public void encodeNewPassword(AppUser user) {
		if (user == null || user.getPassword() == null) {
			log.warn("encodeNewPassword: user/password must not be null");
			throw new IllegalArgumentException("user/password must not be null");
		}
		if (isEncoded(user.getPassword())) {
			log.debug("encodeNewPassword: password already encoded for user id={}", user.getId());
			return;
		}

		log.debug("encodeNewPassword: validating raw password for user id={}", user.getId());
		validator.validateRawPassword(user.getPassword());

		user.setPassword(encoder.encode(user.getPassword()));
		log.debug("encodeNewPassword: password encoded for user id={}", user.getId());
	}

	public void assertCurrentMatches(String providedRaw, String storedEncoded) {
		if (providedRaw == null || storedEncoded == null) {
			log.warn("assertCurrentMatches: providedRaw/storedEncoded must not be null");
			throw new IllegalArgumentException("passwords must not be null");
		}

		if (!encoder.matches(providedRaw, storedEncoded)) {
			log.warn("assertCurrentMatches: current password mismatch");
			throw new IllegalArgumentException("Current password is incorrect");
		}

		log.debug("assertCurrentMatches: current password verified");
	}

	public String encodeForChange(String newRaw) {
		if (newRaw == null) {
			log.warn("encodeForChange: newRaw must not be null");
			throw new IllegalArgumentException("newRaw must not be null");
		}

		log.debug("encodeForChange: validating new raw password");
		validator.validateRawPassword(newRaw);

		var encoded = encoder.encode(newRaw);
		log.debug("encodeForChange: new password encoded");
		return encoded;
	}

	private boolean isEncoded(String p) {
		return p != null && p.startsWith("$2");
	}
}
