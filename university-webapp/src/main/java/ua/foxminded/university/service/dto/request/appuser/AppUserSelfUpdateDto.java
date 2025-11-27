package ua.foxminded.university.service.dto.request.appuser;

import jakarta.validation.constraints.*;
import static ua.foxminded.university.service.dto.request.ValidationConstants.*;

public record AppUserSelfUpdateDto(

        @NotNull
        Long id,

        @Email
        @Size(max = EMAIL_MAX)
        String email,

        @Size(min = PERSON_NAME_MIN, max = PERSON_NAME_MAX)
        @Pattern(
            regexp = PERSON_NAME_REGEX,
            message = PERSON_NAME_MESSAGE
        )
        String firstName,

        @Size(min = PERSON_NAME_MIN, max = PERSON_NAME_MAX)
        @Pattern(
            regexp = PERSON_NAME_REGEX,
            message = PERSON_NAME_MESSAGE
        )
        String lastName
) {}
