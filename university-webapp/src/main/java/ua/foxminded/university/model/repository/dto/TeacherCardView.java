package ua.foxminded.university.model.repository.dto;

import java.time.OffsetDateTime;

import ua.foxminded.university.model.domain.enums.AcademicRank;

public record TeacherCardView(
        Long id,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        OffsetDateTime createdAt,
        AcademicRank academicRank,
        String office
) {}