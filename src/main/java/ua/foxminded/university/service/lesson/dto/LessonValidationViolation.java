package ua.foxminded.university.service.lesson.dto;

public record LessonValidationViolation(
        String field,
        String message
) {}
