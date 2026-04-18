package ua.foxminded.university.web.account.page;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.StudentService;
import ua.foxminded.university.service.TeacherService;

@Component
@RequiredArgsConstructor
public class AccountsPageModelFactory {

    private static final int ACCOUNTS_PER_PAGE = 6;

    private final StudentService studentService;
    private final TeacherService teacherService;

    public AccountsPage build(String view, int pageNumber) {
        var safeView = normalizeView(view);
        var safePage = Math.max(pageNumber, 0);
        var pageable = PageRequest.of(safePage, ACCOUNTS_PER_PAGE);

        if ("teachers".equals(safeView)) {
            var teachersPage = teacherService.listTeacherCardsForAdmin(pageable);

            return new AccountsPage(
                    "Account Management",
                    "Manage sensitive operations for teacher accounts.",
                    "teachers",
                    teachersPage.getNumber(),
                    teachersPage.getTotalPages(),
                    teachersPage.hasPrevious(),
                    teachersPage.hasNext(),
                    List.of(),
                    teachersPage.getContent()
            );
        }

        var studentsPage = studentService.listStudentCardsForAdmin(pageable);

        return new AccountsPage(
                "Account Management",
                "Manage sensitive operations for student accounts.",
                "students",
                studentsPage.getNumber(),
                studentsPage.getTotalPages(),
                studentsPage.hasPrevious(),
                studentsPage.hasNext(),
                studentsPage.getContent(),
                List.of()
        );
    }

    private String normalizeView(String view) {
        return "teachers".equalsIgnoreCase(view) ? "teachers" : "students";
    }
}