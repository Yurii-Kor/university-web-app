package ua.foxminded.university.service.rolechange;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.repository.AppUserRepository;
import ua.foxminded.university.service.rolechange.plan.RoleChangePlan;
import ua.foxminded.university.service.rolechange.plan.TargetRoleChangePlannerRegistry;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleChangePlanningService {

    private final AppUserRepository userRepository;
    private final TargetRoleChangePlannerRegistry plannerRegistry;

    @Transactional(value = TxType.SUPPORTS)
    public RoleChangePlan planRoleChange(long userId, UserRole expectedSourceRole, UserRole targetRole) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Active account not found: id=" + userId));

        assertSourceRoleMatches(user.getRole(), expectedSourceRole, userId);
        assertTargetRoleIsDifferent(user.getRole(), targetRole, userId);

        return plannerRegistry.getRequired(targetRole)
                .plan(userId, user.getRole());
    }

    private void assertSourceRoleMatches(UserRole actualRole, UserRole expectedRole, long userId) {
        if (actualRole == expectedRole) return;

        throw new IllegalStateException(
                "Account role mismatch: userId=" + userId
                        + ", expectedRole=" + expectedRole
                        + ", actualRole=" + actualRole
        );
    }

    private void assertTargetRoleIsDifferent(UserRole sourceRole, UserRole targetRole, long userId) {
        if (sourceRole != targetRole) return;

        throw new IllegalArgumentException(
                "Target role must be different from current role: userId=" + userId
                        + ", role=" + sourceRole
        );
    }
}