package ua.foxminded.university.web.account.delete.strategy;

import ua.foxminded.university.model.domain.enums.UserRole;

public interface AccountDeleter {

    UserRole role();

    void deleteById(long id);

    default void deleteByRoleAndId(UserRole requestedRole, long id) {
        if (role() != requestedRole) {
            throw new IllegalArgumentException(
                    "Invalid account deleter for role=" + requestedRole
                            + ", actual deleter role=" + role());
        }

        deleteById(id);
    }
}
