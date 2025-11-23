package ua.foxminded.university.security;

import lombok.RequiredArgsConstructor;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ua.foxminded.university.model.domain.AppUser;

@Component
@RequiredArgsConstructor
public class PasswordPolicy {

	private static final Logger log = LoggerFactory.getLogger(PasswordPolicy.class);

	private final PasswordEncoder encoder;

	public void encodeNewPassword(AppUser user) {
		Optional.ofNullable(user).orElseThrow(() -> {
			log.warn("encodeNewPassword: user must not be null");
			return new IllegalArgumentException("user/password must not be null");
		});

		var raw = Optional.ofNullable(user.getPassword()).orElseThrow(() -> {
			log.warn("encodeNewPassword: password must not be null ");
			return new IllegalArgumentException("user/password must not be null");
		});

		if (isEncoded(raw)) {
			log.debug("encodeNewPassword: password already encoded");
			return;
		}

		user.setPassword(encoder.encode(raw));
		log.debug("encodeNewPassword: password encoded");
	}

	public void assertCurrentMatches(String provided, String stored) {
		Optional.ofNullable(provided).orElseThrow(() -> {
			log.warn("assertCurrentMatches: providedRaw must not be null");
			return new IllegalArgumentException("passwords must not be null");
		});

		Optional.ofNullable(stored).orElseThrow(() -> {
			log.warn("assertCurrentMatches: storedEncoded must not be null");
			return new IllegalArgumentException("passwords must not be null");
		});

		if (!encoder.matches(provided, stored)) {
			log.warn("assertCurrentMatches: current password mismatch");
			throw new IllegalArgumentException("Current password is incorrect");
		}

		log.debug("assertCurrentMatches: current password verified");
	}

	public String encodePassword(String newRaw) {
		Optional.ofNullable(newRaw).orElseThrow(() -> {
			log.warn("encodeForChange: newRaw must not be null");
			return new IllegalArgumentException("newRaw must not be null");
		});

		log.debug("encodeForChange: validating new raw password");

		var encoded = encoder.encode(newRaw);
		log.debug("encodeForChange: new password encoded");
		return encoded;
	}

	public void assertNewDifferentFromCurrent(String currentRaw, String newRaw) {
		if (Objects.equals(currentRaw, newRaw)) {
			throw new IllegalArgumentException("New password must differ from current password");
		}
	}

	private boolean isEncoded(String p) {
		return Optional.ofNullable(p).map(s -> s.startsWith("$2")).orElse(false);
	}
}
