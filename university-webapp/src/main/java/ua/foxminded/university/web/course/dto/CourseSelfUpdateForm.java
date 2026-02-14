package ua.foxminded.university.web.course.dto;

public record CourseSelfUpdateForm(
        Long id,
        String code,
        String name,
        Long teacherId
) {}
