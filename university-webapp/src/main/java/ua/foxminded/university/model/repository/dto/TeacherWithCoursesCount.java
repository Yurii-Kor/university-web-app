package ua.foxminded.university.model.repository.dto;

import ua.foxminded.university.model.domain.Teacher;

public record TeacherWithCoursesCount(
        Teacher teacher,
        Long coursesCount
) {}