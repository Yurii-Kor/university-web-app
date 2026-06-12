package ua.foxminded.university.service.rolechange.assessment;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.rolechange.assessment.strategy.TargetRoleChangeAssessor;

@Component
public class TargetRoleChangeAssessorRegistry {

    private final Map<UserRole, TargetRoleChangeAssessor> plannersByRole;

    public TargetRoleChangeAssessorRegistry(List<TargetRoleChangeAssessor> planners) {
        this.plannersByRole = planners.stream()
                .collect(Collectors.toMap(TargetRoleChangeAssessor::targetRole, planner -> planner));
    }

    public TargetRoleChangeAssessor getRequired(UserRole targetRole) {
        return Optional.ofNullable(plannersByRole.get(targetRole))
                .orElseThrow(() -> new IllegalStateException(
                        "No role change planner configured for targetRole=" + targetRole));
    }
}
