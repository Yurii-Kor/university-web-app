package ua.foxminded.university.web.course.page;

import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.repository.dto.CourseCardView;

@RequiredArgsConstructor
public enum CoursesPageMode {

    ADMIN(
            "Courses",
            "All courses in the system.",
            true,
            true,
            true,
            true
    ),
    TEACHER(
            "My courses",
            "Courses you teach.",
            false,
            true,
            false,
            false
    ),
    STUDENT(
            "My courses",
            "Courses assigned to your group.",
            true,
            false,
            false,
            false
    );

    private final String pageTitle;
    private final String pageSubtitle;
    private final boolean showTeacherInfo;
    private final boolean allowDescriptionEdit;
    private final boolean showAdminActions;
    private final boolean showCreateButton;

    public CoursesPageModel toPage(Page<CourseCardView> coursesPage) {
        return new CoursesPageModel(
                pageTitle,
                pageSubtitle,
                coursesPage.getContent(),
                showTeacherInfo,
                allowDescriptionEdit,
                showAdminActions,
                showCreateButton,
                coursesPage.getNumber(),
                coursesPage.getTotalPages(),
                coursesPage.hasPrevious(),
                coursesPage.hasNext()
        );
    }

    public static CoursesPageMode fromRoleKey(String roleKey) {
        return switch (roleKey) {
            case "admin" -> ADMIN;
            case "teacher" -> TEACHER;
            case "student" -> STUDENT;
            default -> throw new AccessDeniedException("Unsupported role=" + roleKey);
        };
    }
}