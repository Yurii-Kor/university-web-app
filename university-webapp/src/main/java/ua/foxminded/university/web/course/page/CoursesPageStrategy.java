package ua.foxminded.university.web.course.page;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ua.foxminded.university.model.repository.dto.CourseCardView;

public interface CoursesPageStrategy {
    CoursesPageMode mode();
    Page<CourseCardView> loadCourses(long userId, Pageable pageable);
}