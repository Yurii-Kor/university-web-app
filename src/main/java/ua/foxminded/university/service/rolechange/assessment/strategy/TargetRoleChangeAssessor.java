package ua.foxminded.university.service.rolechange.assessment.strategy;

import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessment;

public interface TargetRoleChangeAssessor {

    UserRole targetRole();

    RoleChangeAssessment assess(long userId, UserRole sourceRole);
}