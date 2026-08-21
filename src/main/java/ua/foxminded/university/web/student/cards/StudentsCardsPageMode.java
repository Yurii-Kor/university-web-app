package ua.foxminded.university.web.student.cards;

import java.util.ArrayList;

import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.persistence.student.projection.StudentCardProjection;

@RequiredArgsConstructor
public enum StudentsCardsPageMode {

    ADMIN(
            "Students",
            "Enabled students in the system.",
            true,
            true,
            true,
            true
    ),

    TEACHER(
            "My students",
            "Students in groups assigned to your courses.",
            true,
            false,
            false,
            false
    ),

    STUDENT(
            "My group",
            "Other students in your study group.",
            false,
            false,
            false,
            false
    );

    private final String pageTitle;
    private final String pageSubtitle;
    private final boolean showGroupInfo;
    private final boolean showEnrollmentYear;
    private final boolean showAdminActions;
    private final boolean showCreateButton;

    public <T extends StudentCardProjection> StudentsCardsPageModel toPage(
            Page<T> studentsPage) {

        return new StudentsCardsPageModel(
                pageTitle,
                pageSubtitle,
                new ArrayList<>(studentsPage.getContent()),
                showGroupInfo,
                showEnrollmentYear,
                showAdminActions,
                showCreateButton,
                studentsPage.getNumber(),
                studentsPage.getTotalPages(),
                studentsPage.hasPrevious(),
                studentsPage.hasNext());
    }

    public static StudentsCardsPageMode fromRoleKey(String roleKey) {

        return switch (roleKey) {
            case "admin" -> ADMIN;
            case "teacher" -> TEACHER;
            case "student" -> STUDENT;
            default -> throw new AccessDeniedException(
                    "Unsupported students cards role="
                            + roleKey);
        };
    }
}