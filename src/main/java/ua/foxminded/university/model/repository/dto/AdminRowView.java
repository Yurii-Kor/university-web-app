package ua.foxminded.university.model.repository.dto;

import java.time.OffsetDateTime;

public record AdminRowView(
        Long id,
        String email,
        String firstName,
        String lastName,
        OffsetDateTime createdAt,
        boolean enabled
) {}
