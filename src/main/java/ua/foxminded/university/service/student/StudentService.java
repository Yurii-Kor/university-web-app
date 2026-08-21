package ua.foxminded.university.service.student;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.persistence.appuser.AppUserRepository;
import ua.foxminded.university.model.persistence.student.StudentRepository;
import ua.foxminded.university.model.persistence.student.projection.AdminStudentCardView;
import ua.foxminded.university.model.persistence.student.projection.StudentCardView;
import ua.foxminded.university.model.persistence.student.projection.StudentPeerCardView;
import ua.foxminded.university.model.persistence.student.projection.StudentProfileView;
import ua.foxminded.university.model.persistence.student.projection.TeacherStudentCardView;
import ua.foxminded.university.model.persistence.studygroup.StudyGroupRepository;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.service.student.dto.StudentCreateDto;
import ua.foxminded.university.service.student.dto.StudentUpdateDto;
import ua.foxminded.university.service.student.exception.StudentCreateException;
import ua.foxminded.university.service.student.exception.StudentUpdateException;
import ua.foxminded.university.service.util.DtoMapper;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    private final StudentRepository studentRepository;
    private final AppUserRepository usersRepository;
    private final StudyGroupRepository groupRepository;

    private final DtoMapper dtoMapper;
    private final PasswordPolicy passwordPolicy;

    @Transactional(value = TxType.REQUIRES_NEW)
    public Student create(StudentCreateDto draft) {
        assertStudentEmailFreeInDb(draft);

        var group = requireGroup(draft);

        var student = dtoMapper.toStudentEntity(draft);
        student.setGroup(group);

        passwordPolicy.encodeNewPassword(student.getUser());

        return studentRepository.save(student);
    }

    public Page<StudentCardView> listStudentCardsForAccountManagement(Pageable pageable) {
        return studentRepository.findStudentCardsForAccountManagement(pageable);
    }

    public Page<AdminStudentCardView> listStudentCardsForAdmin(Pageable pageable) {
        return studentRepository.findStudentCardsForAdmin(pageable);
    }

    public Page<TeacherStudentCardView> listStudentCardsForTeacher(
            long teacherId,
            Pageable pageable) {

        return studentRepository.findStudentCardsForTeacher(teacherId, pageable);
    }

    public Page<StudentPeerCardView> listStudentCardsForStudent(
            long studentId,
            Pageable pageable) {

        return studentRepository.findStudentCardsForStudent(studentId, pageable);
    }

    public StudentProfileView getStudentProfileView(long id) {

        return studentRepository.findStudentProfileViewById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found: id=" + id));
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public Student update(
            long studentId,
            StudentUpdateDto patch) {

        var student = requireStudent(studentId);

        applyGroupPatch(student, patch);
        applyEnrollmentYearPatch(student, patch);

        log.info("Updated student: id={}", studentId);

        return student;
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public void deleteById(long studentId) {
        var student = requireStudent(studentId);

        studentRepository.delete(student);
        log.info("Soft deleted student: id={}", studentId);
    }

    private void applyGroupPatch(Student managed, StudentUpdateDto patch) {

        Optional.ofNullable(patch.groupId())
                .filter(newGroupId -> isDifferentGroup(managed, newGroupId))
                .ifPresent(newGroupId -> {
                    var group = groupRepository.findById(newGroupId)
                            .orElseThrow(() -> new StudentUpdateException(
                                    managed.getId(),
                                    "StudyGroup not found: id=" + newGroupId));

                    managed.setGroup(group);

                    log.debug(
                            "update: group updated (studentId={}, groupId={})",
                            managed.getId(),
                            newGroupId);
                });
    }

    private void applyEnrollmentYearPatch(Student managed, StudentUpdateDto patch) {

        Optional.ofNullable(patch.enrollmentYear())
                .filter(newEnrollmentYear ->
                        !Objects.equals(newEnrollmentYear, managed.getEnrollmentYear()))
                .ifPresent(newEnrollmentYear -> {
                    managed.setEnrollmentYear(newEnrollmentYear);

                    log.debug(
                            "update: enrollment year updated (studentId={}, enrollmentYear={})",
                            managed.getId(),
                            newEnrollmentYear);
                });
    }

    private boolean isDifferentGroup(Student managed, Long newGroupId) {

        return Optional.ofNullable(managed.getGroup())
                .map(group -> !newGroupId.equals(group.getId()))
                .orElse(true);
    }

    private void assertStudentEmailFreeInDb(StudentCreateDto draft) {

        if (usersRepository.existsByEmailIgnoreCase(draft.email())) {
            throw new StudentCreateException(
                    "email",
                    draft,
                    "An account with this email already exists.");
        }
    }

    private StudyGroup requireGroup(StudentCreateDto draft) {

        return groupRepository.findById(draft.groupId())
                .orElseThrow(() ->
                    new StudentCreateException(
                            "groupId",
                            draft,
                            "Selected study group is no longer available."));
    }

    private Student requireStudent(long studentId) {

        return studentRepository.findById(studentId)
                .orElseThrow(() ->
                    new EntityNotFoundException("Student not found: id=" + studentId));
    }
}