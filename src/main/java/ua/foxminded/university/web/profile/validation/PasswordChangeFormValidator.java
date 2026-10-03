package ua.foxminded.university.web.profile.validation;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import ua.foxminded.university.service.appuser.dto.AppUserPasswordChangeDto;


@Component
public class PasswordChangeFormValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return AppUserPasswordChangeDto.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        var form = (AppUserPasswordChangeDto) target;

        var newPassword = Optional.ofNullable(form.newPassword())
                .filter(password -> !password.isBlank());

        var confirmPassword = Optional.ofNullable(form.confirmPassword())
                .filter(password -> !password.isBlank());

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            return;
        }

        if (!Objects.equals(
                newPassword.get(),
                confirmPassword.get())) {

            errors.rejectValue(
                    "confirmPassword",
                    "Mismatch",
                    "Passwords do not match.");
        }
    }
}