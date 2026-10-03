package ua.foxminded.university.service.appuser.dto;

import static ua.foxminded.university.service.util.validation.ValidationConstants.*;

import jakarta.validation.constraints.*;

public record AppUserPasswordChangeDto(

        @NotBlank
        String currentPassword,

        @NotBlank
        @Size(min = PASSWORD_MIN, max = PASSWORD_MAX)
        @Pattern(
            regexp = PASSWORD_REGEX,
            message = PASSWORD_MESSAGE
        )
        String newPassword,

        @NotBlank
        String confirmPassword
) {}
