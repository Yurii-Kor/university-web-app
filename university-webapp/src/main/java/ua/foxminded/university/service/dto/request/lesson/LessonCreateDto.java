package ua.foxminded.university.service.dto.request.lesson;

import jakarta.validation.constraints.*;
import ua.foxminded.university.model.domain.enums.LessonType;

import static ua.foxminded.university.service.dto.request.ValidationConstants.*;

import java.time.OffsetDateTime;

public record LessonCreateDto(
        @NotNull
        Long teacherId,

        @NotNull
        Long courseId,

        @NotNull
        Long groupId,

        @NotNull
        OffsetDateTime startTime,

        @NotNull
        OffsetDateTime endTime,

        @NotBlank
        @Size(max = ROOM_MAX)
        @Pattern(
            regexp = ROOM_CODE_REGEX,
            message = ROOM_MESSAGE
        )
        String room,

        LessonType lessonType,

        String description
) {}
