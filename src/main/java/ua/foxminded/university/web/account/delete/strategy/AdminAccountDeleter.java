package ua.foxminded.university.web.account.delete.strategy;

import org.springframework.stereotype.Component;

import ua.foxminded.university.model.domain.enums.UserRole;

@Component
public class AdminAccountDeleter implements AccountDeleter {

    @Override
    public UserRole role() {
        return UserRole.ADMIN;
    }

    @Override
    public void deleteById(long id) {
        throw new IllegalArgumentException(
                "Admin account deletion is not supported here.");
    }
}
