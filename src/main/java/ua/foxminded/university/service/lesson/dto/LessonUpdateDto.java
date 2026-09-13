package ua.foxminded.university.service.lesson.dto;

import static ua.foxminded.university.service.util.validation.ValidationConstants.ROOM_CODE_REGEX;
import static ua.foxminded.university.service.util.validation.ValidationConstants.ROOM_MAX;
import static ua.foxminded.university.service.util.validation.ValidationConstants.ROOM_MESSAGE;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ua.foxminded.university.model.domain.enums.LessonType;

public record LessonUpdateDto(
		@NotNull
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
		LocalDate date,

		@NotNull
		@DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
		LocalTime startTime,

		@NotNull
		@DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
		LocalTime endTime,

		@NotBlank
		@Size(max = ROOM_MAX)
		@Pattern(
				regexp = ROOM_CODE_REGEX,
				message = ROOM_MESSAGE
		)
		String room,

		@NotNull
		LessonType lessonType,

		String description
) {}