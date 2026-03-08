package ua.foxminded.university.web.util;

import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolationException;

@Component
public class ExceptionMessageReader {
	
	public String formatViolations(ConstraintViolationException ex) {
		return ex.getConstraintViolations()
				.stream()
				.map(v -> v.getPropertyPath() + ": " + v.getMessage())
				.distinct()
				.sorted()
				.collect(Collectors.joining("; "));
	}

	public String safeMessage(Exception ex, String fallback) {
		return Optional.ofNullable(ex.getMessage()).map(String::trim).filter(m -> !m.isBlank()).orElse(fallback);
	}
}
