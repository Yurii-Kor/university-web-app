package ua.foxminded.university.model.persistence.lesson.projection;

import java.time.OffsetDateTime;

import ua.foxminded.university.model.domain.enums.LessonType;

public record LessonScheduleItemView(
        Long id,
        Long courseId,
        String courseCode,
        String courseName,
        Long groupId,
        String groupName,
        Long teacherId,
        String teacherFirstName,
        String teacherLastName,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        String room,
        LessonType lessonType,
        String description
) {}
