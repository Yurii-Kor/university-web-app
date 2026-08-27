package ua.foxminded.university.model.persistence.student.projection;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@EqualsAndHashCode
@ToString
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class StudentCardProjection {

    private final Long id;
    private final String email;
    private final String firstName;
    private final String lastName;
}