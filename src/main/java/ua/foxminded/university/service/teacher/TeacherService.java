package ua.foxminded.university.service.teacher;

import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.persistence.appuser.AppUserRepository;
import ua.foxminded.university.model.persistence.teacher.TeacherRepository;
import ua.foxminded.university.model.persistence.teacher.projection.DeletedTeacherCardProjection;
import ua.foxminded.university.model.persistence.teacher.projection.TeacherRoleChangeCardView;
import ua.foxminded.university.model.persistence.teacher.projection.TeacherDirectoryCardView;
import ua.foxminded.university.model.persistence.teacher.projection.TeacherOptionView;
import ua.foxminded.university.model.persistence.teacher.projection.TeacherProfileView;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.service.teacher.dto.DeletedTeacherCardView;
import ua.foxminded.university.service.teacher.dto.TeacherCreateDto;
import ua.foxminded.university.service.teacher.dto.TeacherUpdateDto;
import ua.foxminded.university.service.util.DtoMapper;
import ua.foxminded.university.service.util.projectionmapper.ProjectionMapper;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherService {

    private static final Logger log = LoggerFactory.getLogger(TeacherService.class);

    private final TeacherRepository teacherRepository;
    private final AppUserRepository usersRepository;
    
    private final ProjectionMapper<DeletedTeacherCardProjection, DeletedTeacherCardView> projectionMapper;
    private final DtoMapper dtoMapper;
    private final PasswordPolicy passwordPolicy;

    @Transactional(value = TxType.REQUIRES_NEW)
    public Teacher create(TeacherCreateDto draft) {

        assertTeacherEmailFreeInDb(draft);

        var teacher = dtoMapper.toTeacherEntity(draft);

        passwordPolicy.encodeNewPassword(teacher.getUser());

        return teacherRepository.save(teacher);
    }

    @Transactional(value = TxType.SUPPORTS)
    public List<Teacher> findByIds(Collection<Long> ids) {
        var distinct = Optional.ofNullable(ids)
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (distinct.isEmpty()) {
            log.warn("findByIds: null/empty input or only nulls after filtering");
            return List.of();
        }

        return teacherRepository.findAllById(distinct);
    }
    
    public Page<TeacherDirectoryCardView> listTeacherCardsForAdmin(Pageable pageable) {
        return teacherRepository.findTeacherCardsForAdmin(pageable);
    }

    public Page<TeacherDirectoryCardView> listTeacherCardsForTeacher(
            long teacherId,
            Pageable pageable) {

        return teacherRepository.findTeacherCardsForTeacher(
                teacherId,
                pageable);
    }

    public Page<TeacherDirectoryCardView> listTeacherCardsForStudent(
            long studentId,
            Pageable pageable) {

        return teacherRepository.findTeacherCardsForStudent(
                studentId,
                pageable);
    }
    
    @Transactional(value = TxType.SUPPORTS)
    public Page<TeacherRoleChangeCardView> listTeacherRoleChangeCardsForAdmin(Pageable pageable) {
        return teacherRepository.findTeacherRoleChangeCardsAll(pageable);
    }

    @Transactional(value = TxType.SUPPORTS)
    public Page<DeletedTeacherCardView> listDeletedTeacherCardsForAdmin(Pageable pageable) {
        return teacherRepository.findDeletedTeacherCards(pageable).map(projectionMapper::toView);
    }

    @Transactional(value = TxType.SUPPORTS)
    public List<TeacherOptionView> listTeacherOptions() {
        return teacherRepository.findTeacherOptions();
    }

    @Transactional(value = TxType.SUPPORTS)
    public TeacherProfileView getTeacherProfileView(long id) {
        return teacherRepository.findTeacherProfileViewById(id)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found: id=" + id));
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public Teacher update(long teacherId, TeacherUpdateDto patch) {
        var teacher = requireTeacher(teacherId);

        applyAcademicRankPatch(teacher, patch);
        applyOfficePatch(teacher, patch);

        log.info("Updated teacher: id={}", teacherId);

        return teacher;
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public void deleteById(long id) {
        var teacher = requireTeacher(id);

        assertTeacherHasNoCourses(id);

        teacherRepository.delete(teacher);
        log.info("Soft deleted teacher: id={}", id);
    }

    private void applyAcademicRankPatch(
            Teacher managed,
            TeacherUpdateDto patch) {

        Optional.ofNullable(patch.academicRank())
                .filter(newAcademicRank ->
                        !Objects.equals(
                                newAcademicRank,
                                managed.getAcademicRank()))
                .ifPresent(newAcademicRank -> {
                    managed.setAcademicRank(newAcademicRank);

                    log.debug(
                            "update: academic rank updated (teacherId={}, academicRank={})",
                            managed.getId(),
                            newAcademicRank);
                });
    }

    private void applyOfficePatch(
            Teacher managed,
            TeacherUpdateDto patch) {

        Optional.ofNullable(patch.office())
                .filter(newOffice ->
                        !Objects.equals(
                                newOffice,
                                managed.getOffice()))
                .ifPresent(newOffice -> {
                    managed.setOffice(newOffice);

                    log.debug(
                            "update: office updated (teacherId={}, office={})",
                            managed.getId(),
                            newOffice);
                });
    }

    private void assertTeacherEmailFreeInDb(TeacherCreateDto draft) {

        if (usersRepository.existsByEmailIgnoreCase(draft.email())) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
    }

    private void assertTeacherHasNoCourses(long teacherId) {
        var coursesCount = teacherRepository.countCoursesByTeacherId(teacherId);

        if (coursesCount == 0) {
            return;
        }

        throw new IllegalStateException(
                "Cannot delete teacher with assigned courses: id="
                        + teacherId
                        + ", coursesCount="
                        + coursesCount);
    }

    private Teacher requireTeacher(long teacherId) {

        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new EntityNotFoundException("Teacher not found: id=" + teacherId));
    }
}
