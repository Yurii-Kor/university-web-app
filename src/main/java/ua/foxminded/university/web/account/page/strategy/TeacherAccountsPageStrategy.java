package ua.foxminded.university.web.account.page.strategy;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.account.page.AccountsPage;
import ua.foxminded.university.web.account.page.AccountsPageStrategy;
import ua.foxminded.university.web.account.page.AccountsPageView;

@Component
@RequiredArgsConstructor
public class TeacherAccountsPageStrategy implements AccountsPageStrategy {

    private final TeacherService teacherService;

    @Override
    public AccountsPageView view() {
        return AccountsPageView.TEACHERS;
    }

    @Override
    public AccountsPage build(Pageable pageable) {
        var teachersPage = teacherService.listTeacherCardsForAdmin(pageable);
        var view = view();

        return new AccountsPage(
                view.pageTitle(),
                view.pageSubtitle(),
                view.currentView(),
                teachersPage.getNumber(),
                teachersPage.getTotalPages(),
                teachersPage.hasPrevious(),
                teachersPage.hasNext(),
                List.of(),
                teachersPage.getContent()
        );
    }
}