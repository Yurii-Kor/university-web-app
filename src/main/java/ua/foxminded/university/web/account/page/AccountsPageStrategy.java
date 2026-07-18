package ua.foxminded.university.web.account.page;

import org.springframework.data.domain.Pageable;

public interface AccountsPageStrategy {
    AccountsPageView view();
    AccountsPage build(Pageable pageable);
}