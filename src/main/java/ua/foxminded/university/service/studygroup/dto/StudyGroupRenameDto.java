package ua.foxminded.university.service.studygroup.dto;

import static ua.foxminded.university.service.util.validation.ValidationConstants.*;

import jakarta.validation.constraints.*;

public record StudyGroupRenameDto(

        @NotNull
        Long id,

        @NotBlank
        @Size(max = GROUP_NAME_MAX)
        @Pattern(
            regexp = GROUP_NAME_REGEX,
            message = GROUP_NAME_MESSAGE
        )
        String name
) {}
