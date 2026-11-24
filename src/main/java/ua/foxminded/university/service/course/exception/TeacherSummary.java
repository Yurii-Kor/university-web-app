package ua.foxminded.university.service.course.exception;

public record TeacherSummary(
        Long id,
        String firstName,
        String lastName,
        String email
) {}