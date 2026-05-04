package ua.foxminded.university.service.rolechange.current.strategy;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.repository.TeacherRepository;
import ua.foxminded.university.service.rolechange.exception.RoleChangeException;

@Component
@RequiredArgsConstructor
public class TeacherCurrentRoleProfileHandler implements CurrentRoleProfileHandler {

    private final TeacherRepository teacherRepository;

    @Override
    public UserRole role() {
        return UserRole.TEACHER;
    }

    @Override
    @Transactional(value = TxType.MANDATORY)
    public void deactivateCurrentProfile(long userId, UserRole targetRole) {
        var teacher = teacherRepository.findById(userId)
                .orElseThrow(() -> new RoleChangeException(
                        userId,
                        UserRole.TEACHER,
                        targetRole,
                        "Active teacher profile not found: userId=" + userId
                ));

        assertTeacherHasNoCourses(userId, targetRole);

        teacher.setDeletedAt(OffsetDateTime.now());
    }

    private void assertTeacherHasNoCourses(long teacherId, UserRole targetRole) {
        var coursesCount = teacherRepository.countCoursesByTeacherId(teacherId);
        if (coursesCount == 0) return;

        throw new RoleChangeException(
                teacherId,
                UserRole.TEACHER,
                targetRole,
                "Cannot change teacher role to " + targetRole
                        + ": teacher has assigned courses, teacherId=" + teacherId
                        + ", coursesCount=" + coursesCount
        );
    }
}