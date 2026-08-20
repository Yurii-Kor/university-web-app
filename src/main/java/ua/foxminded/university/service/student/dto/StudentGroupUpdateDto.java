package ua.foxminded.university.service.student.dto;

import jakarta.validation.constraints.NotNull;

public record StudentGroupUpdateDto(
        @NotNull
        Long groupId
) {}
