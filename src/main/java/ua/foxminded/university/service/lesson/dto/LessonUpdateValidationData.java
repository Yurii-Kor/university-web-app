package ua.foxminded.university.service.lesson.dto;

import java.time.OffsetDateTime;

public record LessonUpdateValidationData(
		Long lessonId,
		Long courseId,
		Long groupId,
		Long assignedTeacherId,
		OffsetDateTime startTime
) {}