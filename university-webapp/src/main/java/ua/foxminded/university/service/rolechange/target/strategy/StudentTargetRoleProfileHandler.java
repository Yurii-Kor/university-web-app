package ua.foxminded.university.service.rolechange.target.strategy;

import java.util.Optional;

import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.repository.StudentRepository;
import ua.foxminded.university.model.repository.StudyGroupRepository;
import ua.foxminded.university.service.dto.request.rolechange.ToStudentRoleChangeDto;
import ua.foxminded.university.service.rolechange.exception.RoleChangeException;
import ua.foxminded.university.service.rolechange.target.TargetRoleProfileData;

@Component
@RequiredArgsConstructor
public class StudentTargetRoleProfileHandler implements TargetRoleProfileHandler {

    private final StudentRepository studentRepository;
    private final StudyGroupRepository groupRepository;

    @Override
    public UserRole role() {
        return UserRole.STUDENT;
    }

    @Override
    @Transactional(value = TxType.MANDATORY)
    public void activateTargetProfile(AppUser user, TargetRoleProfileData data) {
        var restorableGroupId = studentRepository.findRestorableDeletedStudentGroupIdById(user.getId());

        if (restorableGroupId.isPresent()) {
            restoreRestorableDeletedStudent(user, data);
            return;
        }

        Optional.ofNullable(data)
                .orElseThrow(() -> new RoleChangeException(
                        user.getId(),
                        user.getRole(),
                        UserRole.STUDENT,
                        "Student profile data is required because previous student profile is missing or previous group is not active: userId="
                                + user.getId()
                ));

        createStudent(user, requireStudentData(user, data));
    }

    private void restoreRestorableDeletedStudent(AppUser user, TargetRoleProfileData data) {
        var student = studentRepository.findDeletedById(user.getId())
                .orElseThrow(() -> new RoleChangeException(
                        user.getId(),
                        user.getRole(),
                        UserRole.STUDENT,
                        "Deleted student profile not found: userId=" + user.getId()
                ));

        castIfPresent(user, data)
                .ifPresentOrElse(
                        form -> restoreDeletedStudentWithSubmittedData(user, student, form),
                        () -> restoreDeletedStudentWithPreviousData(student)
                );
    }

    private void restoreDeletedStudentWithPreviousData(Student student) {
        student.setDeletedAt(null);
    }

    private void restoreDeletedStudentWithSubmittedData(AppUser user,
                                                       Student student,
                                                       ToStudentRoleChangeDto data) {
        var group = requireActiveGroup(user, data.groupId());

        student.setGroup(group);
        student.setEnrollmentYear(data.enrollmentYear());
        student.setDeletedAt(null);
    }

    private void createStudent(AppUser user, ToStudentRoleChangeDto data) {
        var group = requireActiveGroup(user, data.groupId());

        var student = Student.builder()
                .user(user)
                .group(group)
                .enrollmentYear(data.enrollmentYear())
                .build();

        studentRepository.save(student);
    }

    private StudyGroup requireActiveGroup(AppUser user, Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new RoleChangeException(
                        user.getId(),
                        user.getRole(),
                        UserRole.STUDENT,
                        "Active group not found: id=" + groupId
                ));
    }

    private Optional<ToStudentRoleChangeDto> castIfPresent(AppUser user, TargetRoleProfileData data) {
        return Optional.ofNullable(data).map(value -> {
            if (value instanceof ToStudentRoleChangeDto form) {
                return form;
            }

            throw new RoleChangeException(
                    user.getId(),
                    user.getRole(),
                    UserRole.STUDENT,
                    "Invalid target profile data type for STUDENT: actual=" + value.getClass().getSimpleName()
            );
        });
    }

    private ToStudentRoleChangeDto requireStudentData(AppUser user, TargetRoleProfileData data) {
        return castIfPresent(user, data)
                .orElseThrow(() -> new RoleChangeException(
                        user.getId(),
                        user.getRole(),
                        UserRole.STUDENT,
                        "Student profile data is required."
                ));
    }
}