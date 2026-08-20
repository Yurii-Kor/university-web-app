package ua.foxminded.university.service.student;

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
import ua.foxminded.university.service.student.dto.StudentEnrollmentYearUpdateDto;
import ua.foxminded.university.service.student.dto.StudentGroupUpdateDto;
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
    public void moveToGroup(
            long studentId,
            StudentGroupUpdateDto patch) {

        var student = requireStudent(studentId);
        var targetGroup = requireGroup(studentId, patch.groupId());

        student.setGroup(targetGroup);
        log.info("Moved student to group: studentId={}, groupId={}", 
                studentId, 
                patch.groupId());
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public void updateEnrollmentYear(
            long studentId,
            StudentEnrollmentYearUpdateDto patch) {

        var student = requireStudent(studentId);

        student.setEnrollmentYear(patch.enrollmentYear());
        log.info("Updated student enrollment year: studentId={}, enrollmentYear={}", 
                studentId, 
                patch.enrollmentYear());
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public void deleteById(long studentId) {
        var student = requireStudent(studentId);

        studentRepository.delete(student);
        log.info("Soft deleted student: id={}", studentId);
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

    private StudyGroup requireGroup(long studentId, long groupId) {

        return groupRepository.findById(groupId)
                .orElseThrow(() ->
                    new StudentUpdateException(
                            studentId, 
                            "StudyGroup not found: id=" + groupId));
    }

    private Student requireStudent(long studentId) {

        return studentRepository.findById(studentId)
                .orElseThrow(() ->
                    new EntityNotFoundException("Student not found: id=" + studentId));
    }
}
