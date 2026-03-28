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
import ua.foxminded.university.model.repository.dto.IdCountAgg;
import ua.foxminded.university.model.repository.dto.TeacherOptionView;
import ua.foxminded.university.model.repository.dto.TeacherProfileView;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.service.dto.request.teacher.TeacherCreateDto;
import ua.foxminded.university.service.dto.request.teacher.TeacherSelfUpdateDto;
import ua.foxminded.university.service.dto.response.DeleteResult;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.DtoMapper;
import ua.foxminded.university.service.util.DuplicateGuard;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherService {

	private static final Logger log = LoggerFactory.getLogger(TeacherService.class);

	private final TeacherRepository teacherRepository;
	private final AppUserRepository usersRepository;

	private final EntityValidatior validator;
	private final PasswordPolicy passwordPolicy;

	private final DtoMapper dtoMapper;
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
	public DeleteResult deleteByIds(Collection<Long> ids) {
		var distinct = Optional.ofNullable(ids)
				.orElseGet(Collections::emptyList)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		
		if (distinct.isEmpty()) {
			log.warn("deleteByIds called with null/empty list");
			return new DeleteResult(Set.of(), Set.of());
		}

		assertTeachersHaveNoCourses(distinct);

		var existing = teacherRepository.findAllById(distinct);

		var deletedIds = existing.stream().map(Teacher::getId).collect(Collectors.toSet());
		var notFound = distinct.stream().filter(id -> !deletedIds.contains(id)).collect(Collectors.toSet());

		if (existing.isEmpty()) {
			log.info("deleteByIds: nothing to delete; not found: {}", notFound);
			return new DeleteResult(Set.of(), notFound);
		}

		teacherRepository.deleteAll(existing);
		log.info("Deleted {} teacher(s); not found: {}", deletedIds.size(), notFound);

		return new DeleteResult(deletedIds, notFound);
	}

	private void assertTeachersHaveNoCourses(Collection<Long> ids) {
		var aggs = teacherRepository.countCoursesByTeacherIds(ids);

		var nonEmpty = Optional.ofNullable(aggs)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.filter(a -> Optional.ofNullable(a.count()).orElse(0L) > 0L)
				.sorted(Comparator.comparing(IdCountAgg::id))
				.toList();

		if (!nonEmpty.isEmpty()) {
			var pairs = nonEmpty.stream()
					.map(a -> a.id() + " -> " + a.count())
					.collect(Collectors.joining(", ", "[", "]"));

			log.error("deleteByIds: teachers have courses: {}", pairs);
			throw new IllegalStateException("Cannot delete teachers with courses: " + pairs);
		}
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
