package ua.foxminded.university.web.util;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class DtoFieldNormalizer {

	public String normalizeField(String field) {
		return Optional.ofNullable(field).filter(s -> !s.isBlank()).map(String::trim).orElse(null);
	}

	public String normalizeEmail(String email) {
		return Optional.ofNullable(normalizeField(email)).map(String::toLowerCase).orElse(null);
	}
	
	public String normalizeCode(String code) {
		return Optional.ofNullable(normalizeField(code)).map(String::toUpperCase).orElse(null);
	}
}
