package ua.foxminded.university.model.persistence.student.projection;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class StudentPeerCardView
        extends StudentCardProjection {

    public StudentPeerCardView(
            Long id,
            String email,
            String firstName,
            String lastName) {

        super(
                id,
                email,
                firstName,
                lastName);
    }
}