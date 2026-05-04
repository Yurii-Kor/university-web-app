package ua.foxminded.university.service.rolechange.plan.strategy;

import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.rolechange.plan.RoleChangePlan;

public interface TargetRoleChangePlanner {

    UserRole targetRole();

    RoleChangePlan plan(long userId, UserRole sourceRole);
}