package ua.foxminded.university.service.lesson.dto;

import static ua.foxminded.university.service.util.validation.ValidationConstants.*;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ua.foxminded.university.model.domain.enums.LessonType;

public record LessonUpdateDto(
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