package ua.foxminded.university.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.repository.AppUserRepository;
import ua.foxminded.university.model.repository.StudentRepository;
import ua.foxminded.university.model.repository.StudyGroupRepository;
import ua.foxminded.university.model.repository.TeacherRepository;
import ua.foxminded.university.service.dto.request.appuser.StudentToTeacherRoleChangeDto;
import ua.foxminded.university.service.dto.request.appuser.TeacherToStudentRoleChangeDto;
import ua.foxminded.university.service.exception.appuser.StudentToTeacherRoleChangeException;
import ua.foxminded.university.service.exception.appuser.TeacherToStudentRoleChangeException;
import ua.foxminded.university.service.util.validation.EntityValidatior;

@Service
@RequiredArgsConstructor
@Transactional
public class AppUserRoleTransitionService {

    private static final Logger log = LoggerFactory.getLogger(AppUserRoleTransitionService.class);

    private final AppUserRepository usersRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final StudyGroupRepository groupRepository;
    private final EntityValidatior validator;

    @Transactional(value = TxType.REQUIRES_NEW)
    public void changeStudentToTeacher(StudentToTeacherRoleChangeDto form) {
        validator.validate(form);

        var user = requireUserForStudentToTeacher(form);
        requireStudentUserRole(user, form);

        var student = requireStudentProfile(form);
        assertTeacherProfileAbsent(form);

        var teacher = buildTeacher(user, form);
        teacherRepository.save(teacher);

        user.setRole(UserRole.TEACHER);

        deleteStudentProfile(student.getId(), form);

        log.info("changeStudentToTeacher: role changed successfully (userId={})", form.userId());
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public void changeTeacherToStudent(TeacherToStudentRoleChangeDto form) {
        validator.validate(form);

        var user = requireUserForTeacherToStudent(form);
        requireTeacherUserRole(user, form);

        var teacher = requireTeacherProfile(form);
        assertStudentProfileAbsent(form);
        assertTeacherHasNoAssignedCourses(form);

        var group = requireTargetGroup(form);
        var student = buildStudent(user, group, form);
        studentRepository.save(student);

        user.setRole(UserRole.STUDENT);

        deleteTeacherProfile(teacher.getId(), form);

        log.info("changeTeacherToStudent: role changed successfully (userId={})", form.userId());
    }

    private AppUser requireUserForStudentToTeacher(StudentToTeacherRoleChangeDto form) {
        return usersRepository.findById(form.userId())
                .orElseThrow(() -> new StudentToTeacherRoleChangeException(
                        form, "User not found: id=" + form.userId()));
    }

    private void requireStudentUserRole(AppUser user, StudentToTeacherRoleChangeDto form) {
        Optional.of(user)
                .map(AppUser::getRole)
                .filter(UserRole.STUDENT::equals)
                .orElseThrow(() -> new StudentToTeacherRoleChangeException(
                        form, "User is not a student: id=" + form.userId()));
    }

    private Student requireStudentProfile(StudentToTeacherRoleChangeDto form) {
        return studentRepository.findById(form.userId())
                .orElseThrow(() -> new StudentToTeacherRoleChangeException(
                        form, "Student profile not found: id=" + form.userId()));
    }

    private void assertTeacherProfileAbsent(StudentToTeacherRoleChangeDto form) {
        teacherRepository.findById(form.userId()).ifPresent(t -> {
            throw new StudentToTeacherRoleChangeException(
                    form, "Teacher profile already exists: id=" + form.userId());
        });
    }

    private Teacher buildTeacher(AppUser user, StudentToTeacherRoleChangeDto form) {
        return Teacher.builder()
                .user(user)
                .academicRank(form.academicRank())
                .office(form.office())
                .build();
    }

    private void deleteStudentProfile(long studentId, StudentToTeacherRoleChangeDto form) {
        var deleted = studentRepository.deleteProfileById(studentId);
        if (deleted != 1) {
            throw new StudentToTeacherRoleChangeException(
                    form, "Failed to delete student profile: id=" + form.userId());
        }
    }

    private AppUser requireUserForTeacherToStudent(TeacherToStudentRoleChangeDto form) {
        return usersRepository.findById(form.userId())
                .orElseThrow(() -> new TeacherToStudentRoleChangeException(
                        form, "User not found: id=" + form.userId()));
    }

    private void requireTeacherUserRole(AppUser user, TeacherToStudentRoleChangeDto form) {
        Optional.of(user)
                .map(AppUser::getRole)
                .filter(UserRole.TEACHER::equals)
                .orElseThrow(() -> new TeacherToStudentRoleChangeException(
                        form, "User is not a teacher: id=" + form.userId()));
    }

    private Teacher requireTeacherProfile(TeacherToStudentRoleChangeDto form) {
        return teacherRepository.findById(form.userId())
                .orElseThrow(() -> new TeacherToStudentRoleChangeException(
                        form, "Teacher profile not found: id=" + form.userId()));
    }

    private void assertStudentProfileAbsent(TeacherToStudentRoleChangeDto form) {
        studentRepository.findById(form.userId()).ifPresent(s -> {
            throw new TeacherToStudentRoleChangeException(
                    form, "Student profile already exists: id=" + form.userId());
        });
    }

    private void assertTeacherHasNoAssignedCourses(TeacherToStudentRoleChangeDto form) {
        var assignedCourses = teacherRepository.countCoursesByTeacherId(form.userId());
        if (assignedCourses > 0) {
            throw new TeacherToStudentRoleChangeException(
                    form,
                    "Cannot change role: teacher has assigned courses (count=" + assignedCourses + ")");
        }
    }

    private StudyGroup requireTargetGroup(TeacherToStudentRoleChangeDto form) {
        return groupRepository.findById(form.groupId())
                .orElseThrow(() -> new TeacherToStudentRoleChangeException(
                        form, "StudyGroup not found: id=" + form.groupId()));
    }

    private Student buildStudent(AppUser user, StudyGroup group, TeacherToStudentRoleChangeDto form) {
        return Student.builder()
                .user(user)
                .group(group)
                .enrollmentYear(form.enrollmentYear())
                .build();
    }

    private void deleteTeacherProfile(long teacherId, TeacherToStudentRoleChangeDto form) {
        var deleted = teacherRepository.deleteProfileById(teacherId);
        if (deleted != 1) {
            throw new TeacherToStudentRoleChangeException(
                    form, "Failed to delete teacher profile: id=" + form.userId());
        }
    }
}