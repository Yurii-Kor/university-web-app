package ua.foxminded.university.service.lesson.dto;

import java.time.OffsetDateTime;

import ua.foxminded.university.model.domain.enums.LessonType;

public record RequestedSlot(
        Long groupId,
        Long courseId,
        Long teacherId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String room,
        LessonType lessonType,
        String description
) {
}