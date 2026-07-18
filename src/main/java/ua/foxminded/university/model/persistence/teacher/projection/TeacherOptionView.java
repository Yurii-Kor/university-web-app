package ua.foxminded.university.model.persistence.teacher.projection;

public record TeacherOptionView(
        Long id,
        String firstName,
        String lastName,
        String email
) {}
