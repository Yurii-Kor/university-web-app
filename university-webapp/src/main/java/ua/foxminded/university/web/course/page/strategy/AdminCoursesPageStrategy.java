package ua.foxminded.university.web.course.page.strategy;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.CourseService;
import ua.foxminded.university.web.course.page.CoursesPageModel;
import ua.foxminded.university.web.course.page.CoursesPageStrategy;

@Component
@RequiredArgsConstructor
public class AdminCoursesPageStrategy implements CoursesPageStrategy {

    private final CourseService courseService;

    @Override public String roleKey() { return "admin"; }

    @Override
    public CoursesPageModel build(long userId) {
        return new CoursesPageModel(
                "Courses",
                "All courses in the system.",
                courseService.listCourseCardsForAdmin()
        );
    }
}
