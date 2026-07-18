package ua.foxminded.university.model.persistence.student.projection;

import java.time.OffsetDateTime;

public record StudentProfileView(
		String email,
        String firstName,
        String lastName,
        OffsetDateTime createdAt,
        Integer enrollmentYear,
        String groupName
) {}
