package ua.foxminded.university.service.dto.request.appuser;

import jakarta.validation.constraints.*;
import static ua.foxminded.university.service.dto.request.ValidationConstants.*;

public record AppUserPasswordChangeDto(

        @NotNull
        Long id,

        @NotBlank
        String currentPassword,

        @NotBlank
        @Size(min = PASSWORD_MIN, max = PASSWORD_MAX)
        @Pattern(
            regexp = PASSWORD_REGEX,
            message = PASSWORD_MESSAGE
        )
        String newPassword
) {}
