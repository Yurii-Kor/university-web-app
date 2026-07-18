package ua.foxminded.university.service.util.validation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class EntityValidatior {

    private static final Logger log = LoggerFactory.getLogger(EntityValidatior.class);

    private final Validator validator;

    public <T> void validateWithId(T target, Long entityId) {
        var violations = collectViolations(List.of(requireTarget(target)));

        if (!violations.isEmpty()) {
            log.warn("Entity With Id: {} - violations found: {}", entityId, violations.size());
            throw new ContextConstraintViolationException(entityId, new HashSet<>(violations));
        }
    }

    public <T> void validate(T target) {
        var violations = collectViolations(List.of(requireTarget(target)));

        if (!violations.isEmpty()) {
            log.warn("validate: violations found: {}", violations.size());
            throw new ConstraintViolationException(violations);
        }
    }

    public <T> void validate(T target, Class<?> validationGroup) {
        var violations = collectViolations(List.of(requireTarget(target)), validationGroup);

        if (!violations.isEmpty()) {
            log.warn("validate with group: violations found: {}", violations.size());
            throw new ConstraintViolationException(violations);
        }
    }

    public <T> void validateAll(Collection<T> targets) {
        var violations = collectViolations(targets);

        if (!violations.isEmpty()) {
            log.warn("validateAll: violations found: {}", violations.size());
            throw new ConstraintViolationException(violations);
        }
    }
    
    public <T> Set<ConstraintViolation<?>> collectViolations(T target, Class<?>... groups) {
        return collectViolations(List.of(requireTarget(target)), groups);
    }

    private <T> Set<ConstraintViolation<?>> collectViolations(Collection<T> targets, Class<?>... groups) {
        return Optional.ofNullable(targets)
                .filter(list -> !list.isEmpty())
                .map(list -> {
                    log.debug("validateAll: start, items={}", list.size());

                    var resolvedGroups = resolveGroups(groups);

                    Set<ConstraintViolation<?>> violations = list.stream()
                            .filter(Objects::nonNull)
                            .flatMap(target -> validateTarget(target, resolvedGroups).stream())
                            .collect(Collectors.toSet());

                    if (violations.isEmpty()) {
                        log.debug("validateAll: OK (no violations)");
                    }

                    return violations;
                })
                .orElseGet(Set::of);
    }

    private <T> Set<ConstraintViolation<T>> validateTarget(T target, Class<?>[] groups) {
        return groups.length == 0
                ? validator.validate(target)
                : validator.validate(target, groups);
    }

    private Class<?>[] resolveGroups(Class<?>... groups) {
        return Optional.ofNullable(groups)
                .stream()
                .flatMap(Arrays::stream)
                .filter(Objects::nonNull)
                .toArray(Class<?>[]::new);
    }

    private <T> T requireTarget(T target) {
        return Optional.ofNullable(target)
                .orElseThrow(() -> new IllegalArgumentException("target must not be null"));
    }
}