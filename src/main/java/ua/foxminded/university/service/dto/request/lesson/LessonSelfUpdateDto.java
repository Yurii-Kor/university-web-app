package ua.foxminded.university.service.dto.request.lesson;

import jakarta.validation.constraints.*;
import ua.foxminded.university.model.domain.enums.LessonType;

import static ua.foxminded.university.service.dto.request.ValidationConstants.*;

import java.time.OffsetDateTime;

public record LessonSelfUpdateDto(
        @NotNull
        Long id,

        @NotNull
        Long teacherId,

        Long courseId,

        Long groupId,

        OffsetDateTime startTime,

        OffsetDateTime endTime,

        @Size(max = ROOM_MAX)
        @Pattern(
            regexp = ROOM_CODE_REGEX,
            message = ROOM_MESSAGE
        )
        String room,

        LessonType lessonType,

        String description
) {}
