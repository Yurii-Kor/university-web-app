package ua.foxminded.university.service.course.dto;

import static ua.foxminded.university.service.util.validation.ValidationConstants.DESCRIPTION_MAX;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourseDescriptionUpdateDto(

        @NotNull
        Long id,

        @Size(max = DESCRIPTION_MAX)
        String description
) {}
