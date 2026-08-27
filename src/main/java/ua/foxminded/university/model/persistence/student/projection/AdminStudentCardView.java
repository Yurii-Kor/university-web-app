package ua.foxminded.university.model.persistence.student.projection;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class AdminStudentCardView
        extends StudentCardProjection {

    private final Long groupId;
    private final String groupName;
    private final Integer enrollmentYear;

    public AdminStudentCardView(
            Long id,
            String email,
            String firstName,
            String lastName,
            Long groupId,
            String groupName,
            Integer enrollmentYear) {

        super(
                id,
                email,
                firstName,
                lastName);

        this.groupId = groupId;
        this.groupName = groupName;
        this.enrollmentYear = enrollmentYear;
    }
}