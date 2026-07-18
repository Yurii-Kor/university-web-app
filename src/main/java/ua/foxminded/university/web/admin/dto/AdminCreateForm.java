package ua.foxminded.university.web.admin.dto;

import static ua.foxminded.university.service.util.validation.ValidationConstants.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminCreateForm(

        @NotBlank
        @Email
        @Size(max = EMAIL_MAX)
        String email,

        @NotBlank
        @Size(min = PERSON_NAME_MIN, max = PERSON_NAME_MAX)
        @Pattern(
                regexp = PERSON_NAME_REGEX,
                message = PERSON_NAME_MESSAGE
        )
        String firstName,

        @NotBlank
        @Size(min = PERSON_NAME_MIN, max = PERSON_NAME_MAX)
        @Pattern(
                regexp = PERSON_NAME_REGEX,
                message = PERSON_NAME_MESSAGE
        )
        String lastName,

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