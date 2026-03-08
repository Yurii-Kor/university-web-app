package ua.foxminded.university.web.course.page;

public interface CoursesPageStrategy {
    String roleKey();
    CoursesPageModel build(long userId);
}
