package ua.foxminded.university.service.rolechange.assessment.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessment;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessmentMode;

@Component
public class AdminTargetRoleChangeAssessor implements TargetRoleChangeAssessor {

    @Override
    public UserRole targetRole() {
        return UserRole.ADMIN;
    }

    @Override
    public RoleChangeAssessment assess(long userId, UserRole sourceRole) {
        return blockedPlan(userId, sourceRole);
    }

    private RoleChangeAssessment blockedPlan(long userId, UserRole sourceRole) {
        return new RoleChangeAssessment(
                userId,
                sourceRole,
                UserRole.ADMIN,
                RoleChangeAssessmentMode.BLOCKED,
                "Changing account role to ADMIN is not supported from this workflow.",
                List.of()
        );
    }
}