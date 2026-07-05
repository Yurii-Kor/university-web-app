package ua.foxminded.university.model.repository.dto;

import java.time.OffsetDateTime;

public record StudentProfileView(
		String email,
        String firstName,
        String lastName,
        OffsetDateTime createdAt,
        Integer enrollmentYear,
        String groupName
) {}
