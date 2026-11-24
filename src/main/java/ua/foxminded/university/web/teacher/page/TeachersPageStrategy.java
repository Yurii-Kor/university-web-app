package ua.foxminded.university.web.teacher.page;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ua.foxminded.university.model.persistence.teacher.projection.TeacherDirectoryCardView;

public interface TeachersPageStrategy {

    TeachersPageMode mode();

    Page<TeacherDirectoryCardView> loadTeachers(
            long userId,
            Pageable pageable);
}