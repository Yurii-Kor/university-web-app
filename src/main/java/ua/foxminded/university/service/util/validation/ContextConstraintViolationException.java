package ua.foxminded.university.service.util.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.Optional;
import java.util.Set;

public class ContextConstraintViolationException extends ConstraintViolationException {

    private final Long entityId;

    public ContextConstraintViolationException(Long entityId, Set<ConstraintViolation<?>> violations) {
        super(violations);
        this.entityId = entityId;
    }

    public Optional<Long> entityId() {
        return Optional.ofNullable(entityId);
    }
}