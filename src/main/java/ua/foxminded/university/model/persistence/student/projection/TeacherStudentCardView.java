package ua.foxminded.university.model.persistence.student.projection;

public record TeacherStudentCardView(
        Long id,
        String email,
        String firstName,
        String lastName,
        String groupName
) implements StudentCardProjection {
}
