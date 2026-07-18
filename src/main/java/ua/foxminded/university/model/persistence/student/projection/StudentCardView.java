package ua.foxminded.university.model.persistence.student.projection;

import java.time.OffsetDateTime;

public record StudentCardView(
        Long id,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        OffsetDateTime createdAt,
        Integer enrollmentYear,
        String groupName
) {}