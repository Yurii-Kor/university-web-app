package ua.foxminded.university.service.rolechange.plan.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.rolechange.plan.RoleChangePlan;
import ua.foxminded.university.service.rolechange.plan.RoleChangePlanMode;

@Component
public class AdminTargetRoleChangePlanner implements TargetRoleChangePlanner {

    @Override
    public UserRole targetRole() {
        return UserRole.ADMIN;
    }

    @Override
    public RoleChangePlan plan(long userId, UserRole sourceRole) {
        return blockedPlan(userId, sourceRole);
    }

    private RoleChangePlan blockedPlan(long userId, UserRole sourceRole) {
        return new RoleChangePlan(
                userId,
                sourceRole,
                UserRole.ADMIN,
                RoleChangePlanMode.BLOCKED,
                "Changing account role to ADMIN is not supported from this workflow.",
                List.of()
        );
    }
}