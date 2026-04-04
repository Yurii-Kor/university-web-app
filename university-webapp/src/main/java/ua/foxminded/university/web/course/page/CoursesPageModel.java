package ua.foxminded.university.web.course.page;

import java.util.List;

import ua.foxminded.university.model.repository.dto.CourseCardView;

public record CoursesPageModel(
        String pageTitle,
        String pageSubtitle,
        List<CourseCardView> courses,
        boolean showTeacherInfo,
        boolean allowDescriptionEdit,
        boolean showAdminActions,
        boolean showCreateButton,
        int currentPage,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext
) {}