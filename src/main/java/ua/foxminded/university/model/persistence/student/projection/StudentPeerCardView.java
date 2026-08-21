package ua.foxminded.university.model.persistence.student.projection;

public record StudentPeerCardView(
        Long id,
        String email,
        String firstName,
        String lastName
) implements StudentCardProjection {
}
