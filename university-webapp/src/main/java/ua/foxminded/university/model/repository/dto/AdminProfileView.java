package ua.foxminded.university.model.repository.dto;

import java.time.OffsetDateTime;

public record AdminProfileView(
		String email,
        String firstName,
        String lastName,
        OffsetDateTime createdAt
) {}
