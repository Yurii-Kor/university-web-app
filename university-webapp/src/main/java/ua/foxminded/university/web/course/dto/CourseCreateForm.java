package ua.foxminded.university.web.course.dto;

public record CourseCreateForm(
        String code,
        String name,
        String description,
        Long teacherId
) {}
