package ua.foxminded.university.service.rolechange;

import java.util.Optional;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.repository.AppUserRepository;
import ua.foxminded.university.service.dto.request.rolechange.ToTeacherRoleChangeDto;
import ua.foxminded.university.service.dto.request.rolechange.ToStudentRoleChangeDto;
import ua.foxminded.university.service.rolechange.current.CurrentRoleProfileHandlerRegistry;
import ua.foxminded.university.service.rolechange.exception.RoleChangeException;
import ua.foxminded.university.service.rolechange.target.TargetRoleProfileData;
import ua.foxminded.university.service.rolechange.target.TargetRoleProfileHandlerRegistry;
import ua.foxminded.university.service.util.validation.EntityValidatior;

@RequiredArgsConstructor
@Service
@Transactional
public class RoleChangeService {

    private final AppUserRepository userRepository;
    private final CurrentRoleProfileHandlerRegistry currentRoleHandlerRegistry;
    private final TargetRoleProfileHandlerRegistry targetRoleHandlerRegistry;
    private final EntityValidatior validator;

    public void changeStudentToTeacher(ToTeacherRoleChangeDto form) {
        changeRole(form.userId(), UserRole.STUDENT, UserRole.TEACHER, form);
    }

    public void changeTeacherToStudent(ToStudentRoleChangeDto form) {
        changeRole(form.userId(), UserRole.TEACHER, UserRole.STUDENT, form);
    }

    public void restoreStudentToTeacher(long userId) {
        changeRole(userId, UserRole.STUDENT, UserRole.TEACHER, null);
    }

    public void restoreTeacherToStudent(long userId) {
        changeRole(userId, UserRole.TEACHER, UserRole.STUDENT, null);
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    void changeRole(long userId,
                    UserRole expectedSourceRole,
                    UserRole targetRole,
                    TargetRoleProfileData targetData) {

        Optional.ofNullable(targetData).ifPresent(validator::validate);
        
        assertTargetDataBelongsToUserIfPresent(userId, expectedSourceRole, targetRole, targetData);

        var user = requireActiveUser(userId);
        var sourceRole = user.getRole();

        assertSourceRoleMatches(sourceRole, expectedSourceRole, targetRole, userId);
        assertTargetRoleIsDifferent(sourceRole, targetRole, userId);

        currentRoleHandlerRegistry.getRequired(sourceRole)
                                  .deactivateCurrentProfile(userId, targetRole);

        targetRoleHandlerRegistry.getRequired(targetRole)
                                 .activateTargetProfile(user, targetData);

        user.setRole(targetRole);
    }

    private AppUser requireActiveUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Active account not found: id=" + userId));
    }

    private void assertTargetDataBelongsToUserIfPresent(long userId,
            UserRole expectedSourceRole,
            UserRole targetRole,
            TargetRoleProfileData targetData) {
        
        Optional.ofNullable(targetData)
                .ifPresent(data -> Optional.ofNullable(data.userId())
                .filter(id -> id.equals(userId))
                .orElseThrow(() -> new RoleChangeException(
                    userId,
                    expectedSourceRole,
                    targetRole,
                    "Target profile data userId mismatch: expected=" + userId
                        + ", actual=" + data.userId()
                )));
    }

    private void assertSourceRoleMatches(UserRole actualRole,
            UserRole expectedRole,
            UserRole targetRole,
            long userId) {
        
        if (actualRole == expectedRole) return;

        throw new RoleChangeException(
            userId,
            actualRole,
            targetRole,
            "Account role mismatch: userId=" + userId
                + ", expectedRole=" + expectedRole
                + ", actualRole=" + actualRole
        );
    }

    private void assertTargetRoleIsDifferent(UserRole sourceRole, UserRole targetRole, long userId) {
        if (sourceRole != targetRole) return;

        throw new RoleChangeException(
                userId,
                sourceRole,
                targetRole,
                "Target role must be different from current role: userId=" + userId
                        + ", role=" + sourceRole
        );
    }
}