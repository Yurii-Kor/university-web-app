package ua.foxminded.university.model.persistence.teacher.projection;

import ua.foxminded.university.model.domain.enums.AcademicRank;

public record TeacherDirectoryCardView(
        Long id,
        String email,
        String firstName,
        String lastName,
        AcademicRank academicRank,
        String office) {
}