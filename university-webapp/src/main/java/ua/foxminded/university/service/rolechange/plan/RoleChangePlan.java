package ua.foxminded.university.service.rolechange.plan;

import java.util.List;

import ua.foxminded.university.model.domain.enums.UserRole;

public record RoleChangePlan(
        Long userId,
        UserRole sourceRole,
        UserRole targetRole,
        RoleChangePlanMode mode,
        String message,
        List<String> requiredFields
) {}