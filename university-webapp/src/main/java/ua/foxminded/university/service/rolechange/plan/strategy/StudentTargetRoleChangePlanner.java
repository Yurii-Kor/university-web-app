package ua.foxminded.university.service.rolechange.plan.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.repository.StudentRepository;
import ua.foxminded.university.service.rolechange.plan.RoleChangePlan;
import ua.foxminded.university.service.rolechange.plan.RoleChangePlanMode;

@Component
@RequiredArgsConstructor
public class StudentTargetRoleChangePlanner implements TargetRoleChangePlanner {

    private final StudentRepository studentRepository;

    @Override
    public UserRole targetRole() {
        return UserRole.STUDENT;
    }

    @Override
    public RoleChangePlan plan(long userId, UserRole sourceRole) {
        return studentRepository.findRestorableDeletedStudentGroupIdById(userId).isPresent()
                ? autoRestoreAvailablePlan(userId, sourceRole)
                : inputRequiredPlan(userId, sourceRole);
    }

    private RoleChangePlan autoRestoreAvailablePlan(long userId, UserRole sourceRole) {
        return new RoleChangePlan(
                userId,
                sourceRole,
                UserRole.STUDENT,
                RoleChangePlanMode.AUTO_RESTORE_AVAILABLE,
                "Previous student profile found. It can be restored without additional data.",
                List.of()
        );
    }

    private RoleChangePlan inputRequiredPlan(long userId, UserRole sourceRole) {
        return new RoleChangePlan(
                userId,
                sourceRole,
                UserRole.STUDENT,
                RoleChangePlanMode.INPUT_REQUIRED,
                "Student profile data is required.",
                List.of("groupId", "enrollmentYear")
        );
    }
}