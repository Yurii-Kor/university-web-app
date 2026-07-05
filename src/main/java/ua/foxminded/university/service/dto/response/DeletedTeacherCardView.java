package ua.foxminded.university.service.dto.response;

import java.time.OffsetDateTime;

import ua.foxminded.university.model.domain.enums.AcademicRank;

public record DeletedTeacherCardView(
        Long id,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        OffsetDateTime createdAt,
        OffsetDateTime deletedAt,
        AcademicRank academicRank,
        String office
) {}
