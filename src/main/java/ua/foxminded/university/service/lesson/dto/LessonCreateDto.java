package ua.foxminded.university.service.lesson.dto;

import static ua.foxminded.university.service.util.validation.ValidationConstants.*;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ua.foxminded.university.model.domain.enums.LessonType;

public record LessonCreateDto(
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