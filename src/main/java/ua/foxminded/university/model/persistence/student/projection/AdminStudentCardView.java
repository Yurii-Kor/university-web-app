package ua.foxminded.university.model.persistence.student.projection;

public record AdminStudentCardView(
        Long id,
        String email,
        String firstName,
        String lastName,
        Long groupId,
        String groupName,
        Integer enrollmentYear
) {}
