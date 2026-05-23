package ua.foxminded.university.web.account.validation;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import jakarta.validation.ConstraintViolation;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.rolechange.target.TargetRoleProfileHandlerRegistry;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.web.account.form.RoleChangeForm;

@Component
@RequiredArgsConstructor
public class RoleChangeFormValidator implements Validator {

    private final EntityValidatior entityValidatior;
    private final TargetRoleProfileHandlerRegistry targetRoleHandlerRegistry;

    @Override
    public boolean supports(Class<?> clazz) {
        return RoleChangeForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        var form = requireRoleChangeForm(target);

        rejectViolations(errors, entityValidatior.collectViolations(form));

        if (errors.hasErrors()) {
            return;
        }

        targetRoleHandlerRegistry.targetDataTypeFor(form.targetRole())
                .ifPresentOrElse(
                        group -> rejectViolations(errors, entityValidatior.collectViolations(form, group)),
                        () -> errors.rejectValue(
                                "targetRole",
                                "targetRole.unsupported",
                                "Changing account role to ADMIN is not supported from this workflow."
                        )
                );
    }

    private void rejectViolations(Errors errors, Iterable<ConstraintViolation<?>> violations) {
        violations.forEach(violation -> errors.rejectValue(
                violation.getPropertyPath().toString(),
                violation.getMessageTemplate(),
                violation.getMessage()
        ));
    }

    private RoleChangeForm requireRoleChangeForm(Object target) {
        if (target instanceof RoleChangeForm form) {
            return form;
        }

        throw new IllegalArgumentException(
                "RoleChangeFormValidator supports only RoleChangeForm: actual=" + actualTypeOf(target));
    }

    private String actualTypeOf(Object target) {
        return Optional.ofNullable(target)
                .map(value -> value.getClass().getSimpleName())
                .orElse("null");
    }
}