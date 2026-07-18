package ua.foxminded.university.service.rolechange;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessment;
import ua.foxminded.university.service.rolechange.target.TargetRoleProfileData;
import ua.foxminded.university.service.rolechange.target.TargetRoleProfileHandlerRegistry;
import ua.foxminded.university.service.rolechange.target.strategy.StudentTargetRoleProfileHandler;
import ua.foxminded.university.service.rolechange.target.strategy.TargetRoleProfileHandler;
import ua.foxminded.university.service.rolechange.target.strategy.TeacherTargetRoleProfileHandler;
import ua.foxminded.university.service.rolechange.target.strategy.data.ToStudentRoleProfileData;
import ua.foxminded.university.service.rolechange.target.strategy.data.ToTeacherRoleProfileData;

@Service
@RequiredArgsConstructor
public class RoleChangeFacade {

    private final RoleChangeAssessmentService assessmentService;
    private final RoleChangeService roleChangeService;

    private final StudentTargetRoleProfileHandler studentTargetHandler;
    private final TeacherTargetRoleProfileHandler teacherTargetHandler;
    private final TargetRoleProfileHandlerRegistry targetHandlerRegistry;

    public RoleChangeAssessment assessRoleChange(long userId,
                                                 UserRole expectedSourceRole,
                                                 UserRole targetRole) {

        return assessmentService.assessRoleChange(
                userId,
                expectedSourceRole,
                targetRole
        );
    }

    public void changeRoleToStudent(long userId,
                                    UserRole expectedSourceRole,
                                    ToStudentRoleProfileData targetData) {

        roleChangeService.performRoleChange(
                userId,
                expectedSourceRole,
                studentTargetHandler,
                targetData
        );
    }

    public void changeRoleToTeacher(long userId,
                                    UserRole expectedSourceRole,
                                    ToTeacherRoleProfileData targetData) {

        roleChangeService.performRoleChange(
                userId,
                expectedSourceRole,
                teacherTargetHandler,
                targetData
        );
    }

    public void restoreRole(long userId,
                            UserRole expectedSourceRole,
                            UserRole targetRole) {

        restoreRole(
                userId,
                expectedSourceRole,
                targetHandlerRegistry.getRequired(targetRole)
        );
    }

    private <T extends TargetRoleProfileData> void restoreRole(long userId,
                                                               UserRole expectedSourceRole,
                                                               TargetRoleProfileHandler<T> targetHandler) {

        roleChangeService.performRoleChange(
                userId,
                expectedSourceRole,
                targetHandler,
                null
        );
    }
}