package ua.foxminded.university.model.persistence.student.projection;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class TeacherStudentCardView
        extends StudentCardProjection {

    private final String groupName;

    public TeacherStudentCardView(
            Long id,
            String email,
            String firstName,
            String lastName,
            String groupName) {

        super(
                id,
                email,
                firstName,
                lastName);

        this.groupName = groupName;
    }
}