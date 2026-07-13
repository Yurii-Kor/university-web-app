package ua.foxminded.university.service.rolechange.assessment.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.persistence.teacher.TeacherRepository;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessment;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessmentMode;

@Component
@RequiredArgsConstructor
public class TeacherTargetRoleChangeAssessor implements TargetRoleChangeAssessor {

    private final TeacherRepository teacherRepository;

    @Override
    public UserRole targetRole() {
        return UserRole.TEACHER;
    }

    @Override
    public RoleChangeAssessment assess(long userId, UserRole sourceRole) {
        return teacherRepository.findDeletedById(userId).isPresent()
                ? autoRestoreAvailablePlan(userId, sourceRole)
                : inputRequiredPlan(userId, sourceRole);
    }

    private RoleChangeAssessment autoRestoreAvailablePlan(long userId, UserRole sourceRole) {
        return new RoleChangeAssessment(
                userId,
                sourceRole,
                UserRole.TEACHER,
                RoleChangeAssessmentMode.AUTO_RESTORE_AVAILABLE,
                "Previous teacher profile found. It can be restored without additional data.",
                List.of()
        );
    }

    private RoleChangeAssessment inputRequiredPlan(long userId, UserRole sourceRole) {
        return new RoleChangeAssessment(
                userId,
                sourceRole,
                UserRole.TEACHER,
                RoleChangeAssessmentMode.INPUT_REQUIRED,
                "Teacher profile data is required.",
                List.of("academicRank", "office")
        );
    }
}