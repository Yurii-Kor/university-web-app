package ua.foxminded.university.service.rolechange.target.strategy;

import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.rolechange.target.TargetRoleProfileData;

@Component
public class AdminTargetRoleProfileHandler implements TargetRoleProfileHandler {

    @Override
    public UserRole role() {
        return UserRole.ADMIN;
    }

    @Override
    @Transactional(value = TxType.MANDATORY)
    public void activateTargetProfile(AppUser user, TargetRoleProfileData data) {
        throw new IllegalStateException(
                "Changing account role to ADMIN is not supported from this workflow: userId=" + user.getId());
    }
}