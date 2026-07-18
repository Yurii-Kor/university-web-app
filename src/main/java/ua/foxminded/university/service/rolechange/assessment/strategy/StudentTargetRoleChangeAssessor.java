package ua.foxminded.university.service.rolechange.assessment.strategy;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.persistence.student.StudentRepository;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessment;
import ua.foxminded.university.service.rolechange.assessment.RoleChangeAssessmentMode;

@Component
@RequiredArgsConstructor
public class StudentTargetRoleChangeAssessor implements TargetRoleChangeAssessor {

    private final StudentRepository studentRepository;

    @Override
    public UserRole targetRole() {
        return UserRole.STUDENT;
    }

    @Override
    public RoleChangeAssessment assess(long userId, UserRole sourceRole) {
        return studentRepository.findRestorableDeletedStudentGroupIdById(userId).isPresent()
                ? autoRestoreAvailablePlan(userId, sourceRole)
                : inputRequiredPlan(userId, sourceRole);
    }

    private RoleChangeAssessment autoRestoreAvailablePlan(long userId, UserRole sourceRole) {
        return new RoleChangeAssessment(
                userId,
                sourceRole,
                UserRole.STUDENT,
                RoleChangeAssessmentMode.AUTO_RESTORE_AVAILABLE,
                "Previous student profile found. It can be restored without additional data.",
                List.of()
        );
    }

    private RoleChangeAssessment inputRequiredPlan(long userId, UserRole sourceRole) {
        return new RoleChangeAssessment(
                userId,
                sourceRole,
                UserRole.STUDENT,
                RoleChangeAssessmentMode.INPUT_REQUIRED,
                "Student profile data is required.",
                List.of("groupId", "enrollmentYear")
        );
    }
}