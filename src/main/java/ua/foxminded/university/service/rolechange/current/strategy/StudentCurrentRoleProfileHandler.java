package ua.foxminded.university.service.rolechange.current.strategy;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.repository.StudentRepository;
import ua.foxminded.university.service.rolechange.exception.RoleChangeException;

@Component
@RequiredArgsConstructor
public class StudentCurrentRoleProfileHandler implements CurrentRoleProfileHandler {

    private final StudentRepository studentRepository;

    @Override
    public UserRole role() {
        return UserRole.STUDENT;
    }

    @Override
    @Transactional(value = TxType.MANDATORY)
    public void deactivateCurrentProfile(long userId, UserRole targetRole) {
        var student = studentRepository.findById(userId)
                .orElseThrow(() -> new RoleChangeException(
                        userId,
                        UserRole.STUDENT,
                        targetRole,
                        "Active student profile not found: userId=" + userId
                ));

        student.setDeletedAt(OffsetDateTime.now());
    }
}
