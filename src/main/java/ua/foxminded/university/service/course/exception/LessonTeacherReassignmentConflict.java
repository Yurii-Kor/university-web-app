package ua.foxminded.university.service.course.exception;

import java.time.OffsetDateTime;
import java.util.List;

import ua.foxminded.university.model.persistence.lesson.projection.OverlapHit;

public record LessonTeacherReassignmentConflict(
        Long lessonId,
        Long groupId,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String room,
        List<OverlapHit> teacherConflicts
) {}