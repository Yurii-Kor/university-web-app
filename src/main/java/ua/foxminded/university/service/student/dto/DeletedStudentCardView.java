package ua.foxminded.university.service.student.dto;

import java.time.OffsetDateTime;

public record DeletedStudentCardView(
        Long id,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt,
        Integer enrollmentYear,
        String groupName
) {}