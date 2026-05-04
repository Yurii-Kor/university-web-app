package ua.foxminded.university.service.rolechange.plan;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.rolechange.plan.strategy.TargetRoleChangePlanner;

@Component
public class TargetRoleChangePlannerRegistry {

    private final Map<UserRole, TargetRoleChangePlanner> plannersByRole;

    public TargetRoleChangePlannerRegistry(List<TargetRoleChangePlanner> planners) {
        this.plannersByRole = planners.stream()
                .collect(Collectors.toMap(TargetRoleChangePlanner::targetRole, planner -> planner));
    }

    public TargetRoleChangePlanner getRequired(UserRole targetRole) {
        return Optional.ofNullable(plannersByRole.get(targetRole))
                .orElseThrow(() -> new IllegalStateException(
                        "No role change planner configured for targetRole=" + targetRole));
    }
}
