package ua.foxminded.university.model.persistence.studygroup.projection;

public record GroupCardView(
        Long id,
        String name,
        long studentCount,
        long courseCount
) {}
