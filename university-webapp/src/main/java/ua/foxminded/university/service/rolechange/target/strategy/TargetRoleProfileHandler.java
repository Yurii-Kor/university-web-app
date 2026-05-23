package ua.foxminded.university.service.rolechange.target.strategy;

import java.util.Optional;

import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.rolechange.target.TargetRoleProfileData;

public interface TargetRoleProfileHandler {

    UserRole role();
    
    Optional<Class<? extends TargetRoleProfileData>> targetDataType();

    void activateTargetProfile(AppUser user, TargetRoleProfileData data);
}