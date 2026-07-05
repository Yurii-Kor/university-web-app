package ua.foxminded.university.service.rolechange.assessment;

import java.util.List;

import ua.foxminded.university.model.domain.enums.UserRole;

public record RoleChangeAssessment(
        Long userId,
        UserRole sourceRole,
        UserRole targetRole,
        RoleChangeAssessmentMode mode,
        String message,
        List<String> requiredFields
) {}