package ua.foxminded.university.web.account.page;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class AccountsPageModelFactory {

    private static final int ACCOUNTS_PER_PAGE = 6;

    private final Map<AccountsPageView, AccountsPageStrategy> strategiesByView;

    public AccountsPageModelFactory(List<AccountsPageStrategy> strategies) {
        this.strategiesByView = strategies.stream()
                .collect(Collectors.toMap(AccountsPageStrategy::view, s -> s));
    }

    public AccountsPage build(String view, int pageNumber) {
        var safeView = AccountsPageView.fromView(view);
        var safePage = Math.max(pageNumber, 0);
        var pageable = PageRequest.of(safePage, ACCOUNTS_PER_PAGE);

        var strategy = Optional.ofNullable(strategiesByView.get(safeView))
                .orElseThrow(() -> new IllegalStateException(
                        "No accounts page strategy configured for view=" + safeView));

        return strategy.build(pageable);
    }
}