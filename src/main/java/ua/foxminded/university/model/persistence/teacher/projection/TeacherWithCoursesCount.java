package ua.foxminded.university.model.persistence.teacher.projection;

import ua.foxminded.university.model.domain.Teacher;

public record TeacherWithCoursesCount(
        Teacher teacher,
        Long coursesCount
) {}