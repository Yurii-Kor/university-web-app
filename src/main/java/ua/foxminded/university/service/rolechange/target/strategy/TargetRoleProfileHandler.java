package ua.foxminded.university.service.rolechange.target.strategy;

import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.rolechange.target.TargetRoleProfileData;

public interface TargetRoleProfileHandler<T extends TargetRoleProfileData> {

    UserRole role();

    void activateTargetProfile(AppUser user, T data);
}