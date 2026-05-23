package ua.foxminded.university.service.rolechange;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessment;
import ua.foxminded.university.service.rolechange.target.TargetRoleProfileData;

@Service
@RequiredArgsConstructor
public class RoleChangeFacade {

    private final RoleChangeAssessmentService assessmentService;
    private final RoleChangeService roleChangeService;

    public RoleChangeAssessment assessRoleChange(long userId,
                                                 UserRole expectedSourceRole,
                                                 UserRole targetRole) {

        return assessmentService.assessRoleChange(userId, expectedSourceRole, targetRole);
    }

    public void changeRole(long userId,
                           UserRole expectedSourceRole,
                           UserRole targetRole,
                           TargetRoleProfileData targetData) {

        roleChangeService.performRoleChange(
                userId,
                expectedSourceRole,
                targetRole,
                targetData
        );
    }

    public void restoreRole(long userId,
                            UserRole expectedSourceRole,
                            UserRole targetRole) {

        roleChangeService.performRoleChange(
                userId,
                expectedSourceRole,
                targetRole,
                null
        );
    }
}