package ua.foxminded.university.service.rolechange.plan.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.repository.TeacherRepository;
import ua.foxminded.university.service.rolechange.plan.RoleChangePlan;
import ua.foxminded.university.service.rolechange.plan.RoleChangePlanMode;

@Component
@RequiredArgsConstructor
public class TeacherTargetRoleChangePlanner implements TargetRoleChangePlanner {

    private final TeacherRepository teacherRepository;

    @Override
    public UserRole targetRole() {
        return UserRole.TEACHER;
    }

    @Override
    public RoleChangePlan plan(long userId, UserRole sourceRole) {
        return teacherRepository.findDeletedById(userId).isPresent()
                ? autoRestoreAvailablePlan(userId, sourceRole)
                : inputRequiredPlan(userId, sourceRole);
    }

    private RoleChangePlan autoRestoreAvailablePlan(long userId, UserRole sourceRole) {
        return new RoleChangePlan(
                userId,
                sourceRole,
                UserRole.TEACHER,
                RoleChangePlanMode.AUTO_RESTORE_AVAILABLE,
                "Previous teacher profile found. It can be restored without additional data.",
                List.of()
        );
    }

    private RoleChangePlan inputRequiredPlan(long userId, UserRole sourceRole) {
        return new RoleChangePlan(
                userId,
                sourceRole,
                UserRole.TEACHER,
                RoleChangePlanMode.INPUT_REQUIRED,
                "Teacher profile data is required.",
                List.of("academicRank", "office")
        );
    }
}