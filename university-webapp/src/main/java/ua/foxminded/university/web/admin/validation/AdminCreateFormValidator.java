package ua.foxminded.university.web.admin.validation;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ua.foxminded.university.web.admin.dto.AdminCreateForm;

@Component
public class AdminCreateFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return AdminCreateForm.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        var form = (AdminCreateForm) target;

        var newPassword = Optional.ofNullable(form.newPassword())
                .filter(s -> !s.isBlank());

        var confirmPassword = Optional.ofNullable(form.confirmPassword())
                .filter(s -> !s.isBlank());

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            return;
        }

        if (!Objects.equals(newPassword.get(), confirmPassword.get())) {
            errors.rejectValue("confirmPassword", "Mismatch", "Passwords do not match.");
        }
    }
}