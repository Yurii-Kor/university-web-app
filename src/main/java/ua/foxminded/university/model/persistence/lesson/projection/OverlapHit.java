package ua.foxminded.university.model.persistence.lesson.projection;

import java.time.OffsetDateTime;

public record OverlapHit(
        Long entryId,
        Long groupId,
        Long courseId,
        Long teacherId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String room
) {}
