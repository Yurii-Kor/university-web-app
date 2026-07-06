package ua.foxminded.university.service;

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
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.repository.AppUserRepository;
import ua.foxminded.university.model.repository.TeacherRepository;
import ua.foxminded.university.model.repository.dto.TeacherCardView;
import ua.foxminded.university.model.repository.dto.DeletedTeacherCardProjection;
import ua.foxminded.university.model.repository.dto.TeacherOptionView;
import ua.foxminded.university.model.repository.dto.TeacherProfileView;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.service.dto.request.teacher.TeacherCreateDto;
import ua.foxminded.university.service.dto.request.teacher.TeacherSelfUpdateDto;
import ua.foxminded.university.service.dto.response.DeletedTeacherCardView;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.DtoMapper;
import ua.foxminded.university.service.util.DuplicateGuard;
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
	private final EntityValidatior validator;
	private final PasswordPolicy passwordPolicy;
	private final DuplicateGuard duplicateGuard;

	@Transactional(value = TxType.REQUIRES_NEW)
	public List<Teacher> createAll(Collection<TeacherCreateDto> drafts) {
		drafts = Optional.ofNullable(drafts).orElseGet(List::of).stream().filter(Objects::nonNull).toList();
		if (drafts.isEmpty()) {
			log.warn("createAll: nothing to persist (null/empty input)");
			return List.of();
		}

		validator.validateAll(drafts);

		var toPersist = dtoMapper.toTeacherEntities(drafts);

		var emailsLower = toPersist.stream()
				.map(Teacher::getUser)
				.map(AppUser::getEmail)
				.map(String::toLowerCase)
				.toList();

		duplicateGuard.assertNoDuplicates(emailsLower, "normalized teacher emails");
		assertTeacherEmailsFreeInDb(emailsLower);

		toPersist.forEach(t -> passwordPolicy.encodeNewPassword(t.getUser()));

		return teacherRepository.saveAll(toPersist);
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
	
	@Transactional(value = TxType.SUPPORTS)
	public Page<TeacherCardView> listTeacherCardsForAdmin(Pageable pageable) {
	    return teacherRepository.findTeacherCardsAll(pageable);
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
	public Teacher updateSelf(TeacherSelfUpdateDto patch) {
		patch = Optional.ofNullable(patch).orElseThrow(() -> new IllegalArgumentException("patch must not be null"));

		validator.validate(patch);
		
		var id = patch.id();
		var managed = teacherRepository.findById(id).orElseThrow(() -> {
			log.error("updateSelf: Teacher not found: id={}", id);
			return new EntityNotFoundException("Teacher not found: id=" + id);
		});

		Optional.ofNullable(patch.academicRank()).ifPresent(managed::setAcademicRank);

		Optional.ofNullable(patch.office()).ifPresent(managed::setOffice);

		log.debug("updateSelf: updated rank/office for teacherId={}", managed.getId());
		return managed;
	}

    @Transactional(value = TxType.REQUIRES_NEW)
    public void deleteById(long id) {
        var teacher = teacherRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Active teacher not found: id=" + id));

        assertTeacherHasNoCourses(id);

        teacherRepository.delete(teacher);
        log.info("Soft deleted teacher: id={}", id);
    }

    private void assertTeacherHasNoCourses(long teacherId) {
        var coursesCount = teacherRepository.countCoursesByTeacherId(teacherId);
        if (coursesCount == 0) return;
        
        throw new IllegalStateException(
                "Cannot delete teacher with assigned courses: id=" + teacherId + ", coursesCount=" + coursesCount);
    }

	private void assertTeacherEmailsFreeInDb(Collection<String> emailsLower) {
		var normalized = emailsLower.stream()
				.filter(Objects::nonNull)
				.map(String::trim)
				.map(String::toLowerCase)
				.collect(Collectors.toSet());

		if (normalized.isEmpty())
			return;

		var conflicts = usersRepository.findExistingEmailsIgnoreCase(normalized);
		if (!conflicts.isEmpty()) {
			log.warn("createTeachers: emails already exist in DB: {}", conflicts);
			throw new IllegalArgumentException("Emails already exist: " + conflicts);
		}
	}
}
