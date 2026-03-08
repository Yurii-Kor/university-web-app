package ua.foxminded.university.service.util.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class EntityValidatior {

    private static final Logger log = LoggerFactory.getLogger(EntityValidatior.class);

    private final Validator validator;

    public <T> void validateWithId(T target, Long entityId) {
		Optional.ofNullable(target).orElseThrow(() -> new IllegalArgumentException("target must not be null"));

        var violations = validator.validate(target);
        if (!violations.isEmpty()) {
        	log.warn("Entety With Id: {} - violations found: {}", entityId, violations.size());
            throw new ContextConstraintViolationException(entityId, new HashSet<>(violations));
        }
    }

    public <T> void validate(T target) {
        var singletonOrEmpty = Optional.ofNullable(target).map(List::of).orElseGet(Collections::emptyList);
        validateAll(singletonOrEmpty);
    }

    public <T> void validateAll(Collection<T> targets) {
        Optional.ofNullable(targets).filter(list -> !list.isEmpty()).ifPresent(list -> {
            log.debug("validateAll: start, items={}", list.size());

            Set<ConstraintViolation<?>> violations = list.stream()
                    .filter(Objects::nonNull)
                    .flatMap(t -> validator.validate(t).stream())
                    .collect(Collectors.toSet());

            if (!violations.isEmpty()) {
                log.warn("validateAll: violations found: {}", violations.size());
                throw new ConstraintViolationException(violations);
            }

            log.debug("validateAll: OK (no violations)");
        });
    }
}