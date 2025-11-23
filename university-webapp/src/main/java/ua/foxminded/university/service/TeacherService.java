package ua.foxminded.university.service;

import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.repository.AppUserRepository;
import ua.foxminded.university.model.repository.TeacherRepository;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.service.dto.request.TeacherDto;
import ua.foxminded.university.service.dto.response.DeleteResult;
import ua.foxminded.university.service.util.RequestDtoNormalizer;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.validation.groups.OnCreate;
import ua.foxminded.university.service.util.validation.groups.OnUpdateSelf;
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

	private final RequestDtoNormalizer normalizer;
	private final DtoMapper dtoMapper;
	private final DuplicateGuard duplicateGuard;

	@Transactional(value = TxType.REQUIRES_NEW)
	public List<Teacher> createAll(Collection<TeacherDto> drafts) {
		var normalized = normalizer.normalizeTeachers(drafts);
		validator.validateAll(normalized, OnCreate.class);

		var toPersist = dtoMapper.toTeacherEntities(normalized);
		if (toPersist.isEmpty()) {
			log.warn("createAll: nothing to persist (null/empty input)");
			return List.of();
		}

		var emailsLower = toPersist.stream()
				.map(Teacher::getUser)
				.filter(Objects::nonNull)
				.map(AppUser::getEmail)
				.filter(Objects::nonNull)
				.map(String::trim)
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

	@Transactional(value = TxType.REQUIRES_NEW)
	public Teacher updateSelf(TeacherDto patch) {
		var normalized = normalizer.normalizeTeacher(patch)
				.orElseThrow(() -> new IllegalArgumentException("patch must not be null"));

		validator.validateAll(List.of(normalized), OnUpdateSelf.class);

		var managed = teacherRepository.findById(normalized.id()).orElseThrow(() -> {
			log.error("updateSelf: Teacher not found: id={}", normalized.id());
			return new EntityNotFoundException("Teacher not found: id=" + normalized.id());
		});

		Optional.ofNullable(normalized.academicRank()).ifPresent(managed::setAcademicRank);

		Optional.ofNullable(normalized.office()).ifPresent(managed::setOffice);

		log.debug("updateSelf: updated rank/office for teacherId={}", managed.getId());
		return managed;
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public DeleteResult deleteByIds(Collection<Long> ids) {
		if (Optional.ofNullable(ids).map(Collection::isEmpty).orElse(true)) {
			log.warn("deleteByIds called with null/empty list");
			return new DeleteResult(Set.of(), Set.of());
		}

		var distinct = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());

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
				.sorted(Comparator.comparing(ua.foxminded.university.model.repository.dto.IdCountAgg::id))
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
