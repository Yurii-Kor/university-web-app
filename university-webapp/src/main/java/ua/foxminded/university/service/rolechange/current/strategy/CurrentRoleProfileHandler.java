package ua.foxminded.university.service.rolechange.current.strategy;

import ua.foxminded.university.model.domain.enums.UserRole;

public interface CurrentRoleProfileHandler {

    UserRole role();

    void deactivateCurrentProfile(long userId, UserRole targetRole);
}