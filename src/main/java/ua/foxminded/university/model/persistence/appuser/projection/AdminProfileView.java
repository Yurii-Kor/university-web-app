package ua.foxminded.university.model.persistence.appuser.projection;

import java.time.OffsetDateTime;

public record AdminProfileView(
		String email,
        String firstName,
        String lastName,
        OffsetDateTime createdAt
) {}
