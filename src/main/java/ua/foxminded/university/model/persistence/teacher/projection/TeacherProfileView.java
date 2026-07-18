package ua.foxminded.university.model.persistence.teacher.projection;

import java.time.OffsetDateTime;

import ua.foxminded.university.model.domain.enums.AcademicRank;

public record TeacherProfileView(
		String email,
        String firstName,
        String lastName,
        OffsetDateTime createdAt,
        AcademicRank academicRank,
        String office
) {}
