package ua.foxminded.university.model.repository.dto;

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
