package ua.foxminded.university.service.dto.request.course;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static ua.foxminded.university.service.dto.request.ValidationConstants.DESCRIPTION_MAX;

public record CourseDescriptionUpdateDto(

        @NotNull
        Long id,

        @Size(max = DESCRIPTION_MAX)
        String description
) {}
