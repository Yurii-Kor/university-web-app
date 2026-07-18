package ua.foxminded.university.model.persistence.course.projection;

public record CourseHeaderView(
        Long id,
        String code,
        String name
) {}
