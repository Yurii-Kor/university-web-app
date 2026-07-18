package ua.foxminded.university.service.rolechange.current.strategy;

import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import ua.foxminded.university.model.domain.enums.UserRole;

@Component
public class AdminCurrentRoleProfileHandler implements CurrentRoleProfileHandler {

    @Override
    public UserRole role() {
        return UserRole.ADMIN;
    }

    @Override
    @Transactional(value = TxType.MANDATORY)
    public void deactivateCurrentProfile(long userId, UserRole targetRole) {
        throw new IllegalStateException(
                "Admin role cannot be changed: userId=" + userId
                        + ", targetRole=" + targetRole
        );
    }
}
