package ua.foxminded.university.web.account.delete;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.web.account.delete.strategy.AccountDeleter;

@Component
public class AccountDeleterRegistry {

    private final Map<UserRole, AccountDeleter> deletersByRole;

    public AccountDeleterRegistry(List<AccountDeleter> deleters) {
        this.deletersByRole = deleters.stream()
                .collect(Collectors.toMap(AccountDeleter::role, deleter -> deleter));
    }

    public AccountDeleter getRequired(UserRole role) {
        return Optional.ofNullable(deletersByRole.get(role))
                .orElseThrow(() -> new IllegalArgumentException(
                        "No account deleter configured for role=" + role));
    }
}