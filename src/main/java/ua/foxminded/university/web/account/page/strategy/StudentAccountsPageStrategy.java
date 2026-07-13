package ua.foxminded.university.web.account.page.strategy;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.student.StudentService;
import ua.foxminded.university.web.account.page.AccountsPage;
import ua.foxminded.university.web.account.page.AccountsPageStrategy;
import ua.foxminded.university.web.account.page.AccountsPageView;

@Component
@RequiredArgsConstructor
public class StudentAccountsPageStrategy implements AccountsPageStrategy {

    private final StudentService studentService;

    @Override
    public AccountsPageView view() {
        return AccountsPageView.STUDENTS;
    }

    @Override
    public AccountsPage build(Pageable pageable) {
        var studentsPage = studentService.listStudentCardsForAdmin(pageable);
        var view = view();

        return new AccountsPage(
                view.pageTitle(),
                view.pageSubtitle(),
                view.currentView(),
                studentsPage.getNumber(),
                studentsPage.getTotalPages(),
                studentsPage.hasPrevious(),
                studentsPage.hasNext(),
                studentsPage.getContent(),
                List.of()
        );
    }
}