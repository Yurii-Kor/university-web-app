package ua.foxminded.university.web.course.page.strategy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.persistence.course.projection.CourseCardView;
import ua.foxminded.university.service.course.CourseService;
import ua.foxminded.university.web.course.page.CoursesPageMode;
import ua.foxminded.university.web.course.page.CoursesPageStrategy;

@Component
@RequiredArgsConstructor
public class AdminCoursesPageStrategy implements CoursesPageStrategy {

    private final CourseService courseService;

    @Override
    public CoursesPageMode mode() {
        return CoursesPageMode.ADMIN;
    }

    @Override
    public Page<CourseCardView> loadCourses(long userId, Pageable pageable) {
        return courseService.listCourseCardsForAdmin(pageable);
    }
}