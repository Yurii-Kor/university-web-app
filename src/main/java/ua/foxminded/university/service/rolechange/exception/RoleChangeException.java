package ua.foxminded.university.service.rolechange.exception;

import ua.foxminded.university.model.domain.enums.UserRole;

public class RoleChangeException extends RuntimeException {

    private final long accountId;
    private final UserRole currentRole;
    private final UserRole targetRole;

    public RoleChangeException(long accountId,
                               UserRole currentRole,
                               UserRole targetRole,
                               String message) {
        super(message);
        this.accountId = accountId;
        this.currentRole = currentRole;
        this.targetRole = targetRole;
    }

    public long accountId() {
        return accountId;
    }

    public UserRole currentRole() {
        return currentRole;
    }

    public UserRole targetRole() {
        return targetRole;
    }
}