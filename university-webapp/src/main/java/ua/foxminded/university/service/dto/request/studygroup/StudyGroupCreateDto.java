package ua.foxminded.university.service.dto.request.studygroup;

import jakarta.validation.constraints.*;

import static ua.foxminded.university.service.dto.request.ValidationConstants.*;

public record StudyGroupCreateDto(

		@NotBlank
        @Size(max = GROUP_NAME_MAX)
        @Pattern(
            regexp = GROUP_NAME_REGEX,
            message = GROUP_NAME_MESSAGE
        )
        String name
) {}
