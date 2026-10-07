package ua.foxminded.university.model.persistence.studygroup.projection;

public record StudentGroupHeaderView(
        Long id,
        String name,
        long studentCount
) {}