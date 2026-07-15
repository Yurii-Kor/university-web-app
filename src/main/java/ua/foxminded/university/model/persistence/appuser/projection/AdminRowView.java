package ua.foxminded.university.model.persistence.appuser.projection;

import java.time.OffsetDateTime;

public record AdminRowView(
        Long id,
        String email,
        String firstName,
        String lastName,
        OffsetDateTime createdAt,
        boolean enabled
) {}
