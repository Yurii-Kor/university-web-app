package ua.foxminded.university.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.domain.validation.EntityValidatior;
import ua.foxminded.university.model.repository.AppUserRepository;
import ua.foxminded.university.model.repository.TeacherRepository;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.service.dto.DeleteResult;
import jakarta.persistence.EntityNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherService {
	
	private final Integer NOT_UPDATED  = 0;

	private static final Logger log = LoggerFactory.getLogger(TeacherService.class);

	private final TeacherRepository teacherRepository;
	private final AppUserRepository usersRepository;
	
	private final EntityValidatior validator;
	private final PasswordPolicy passwordPolicy;

	@Transactional(value = TxType.REQUIRES_NEW)
	public List<Teacher> createAll(Collection<Teacher> teachers) {
		var toPersist = normalizeTeachersToPersist(teachers);
		if (toPersist.isEmpty()) {
			log.warn("createAll: nothing to persist (null/empty input or all items null)");
			return List.of();
		}
		
		assertNoDuplicateEmailsInRequest(teachers);
		assertTeacherEmailsFreeInDb(teachers);

		validator.validateAll(toPersist);
		toPersist.forEach(t -> passwordPolicy.encodeNewPassword(t.getUser()));

		return teacherRepository.saveAll(toPersist);
	}

	@Transactional(value = TxType.SUPPORTS)
	public List<Teacher> findByIds(Collection<Long> ids) {
		var distinct = Optional.ofNullable(ids)
				.orElseGet(Collections::emptySet)
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
	public int updateOffices(Map<Long, String> officeByTeacherId) {
		if (Optional.ofNullable(officeByTeacherId).map(Map::isEmpty).orElse(true)) {
			log.warn("updateOffices: empty/null map");
			return NOT_UPDATED;
		}
		assertIdsNotNull(officeByTeacherId);

		var normalized = normalizeOffices(officeByTeacherId);

		validator.validateTeachersOffices(List.copyOf(normalized.values()));

		var ids = List.copyOf(normalized.keySet());
		var existing = teacherRepository.findAllById(ids);

		var found = existing.stream().map(Teacher::getId).collect(Collectors.toSet());
		var missing = ids.stream().filter(id -> !found.contains(id)).collect(Collectors.toSet());
		
		Optional.of(missing).filter(Predicate.not(Collection::isEmpty)).ifPresent(m -> {
			log.error("updateOffices: missing teacher ids {}", m);
			throw new EntityNotFoundException("Teachers not found: " + m);
		});

		existing.forEach(t -> t.setOffice(normalized.get(t.getId())));

		log.info("updateOffices: updated {}", existing.size());
		return existing.size();
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public int updateAcademicRanks(Map<Long, AcademicRank> rankByTeacherId) {
		if (Optional.ofNullable(rankByTeacherId).map(Map::isEmpty).orElse(true)) {
			log.warn("updateAcademicRanks: empty/null map");
			return NOT_UPDATED;
		}

		assertIdsNotNull(rankByTeacherId);
		assertAcademicRanksNotNull(rankByTeacherId);

		var ids = List.copyOf(rankByTeacherId.keySet());
		var existing = teacherRepository.findAllById(ids);

		var found = existing.stream().map(Teacher::getId).collect(Collectors.toSet());
		var missing = ids.stream().filter(id -> !found.contains(id)).collect(Collectors.toSet());
		
		Optional.of(missing).filter(Predicate.not(Collection::isEmpty)).ifPresent(m -> {
			log.error("updateAcademicRanks: missing teacher ids {}", m);
			throw new EntityNotFoundException("Teachers not found: " + m);
		});

		existing.forEach(t -> t.setAcademicRank(rankByTeacherId.get(t.getId())));
		log.info("updateAcademicRanks: updated {}", existing.size());
		return existing.size();
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public DeleteResult deleteByIds(Collection<Long> ids) {
		if (Optional.ofNullable(ids).map(Collection::isEmpty).orElse(true)) {
			log.warn("deleteByIds called with null/empty list");
			return new DeleteResult(Set.of(), Set.of());
		}

		var distinct = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
		var existing = teacherRepository.findAllById(distinct);

		var deletedIds = existing.stream().map(Teacher::getId).collect(Collectors.toSet());

		var notFound = distinct.stream().filter(id -> !deletedIds.contains(id)).collect(Collectors.toSet());

		teacherRepository.deleteAll(existing);
		log.info("Deleted {} teacher(s); not found: {}", deletedIds.size(), notFound);

		return new DeleteResult(deletedIds, notFound);
	}
	
	private List<Teacher> normalizeTeachersToPersist(Collection<Teacher> teachersToPersist) {
		return Optional.ofNullable(teachersToPersist)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.map(this::normalizeTeacherToPersist)
				.toList();
	}

	private Teacher normalizeTeacherToPersist(Teacher t) {
		t.setId(null);

		Optional.ofNullable(t.getUser()).ifPresent(u -> {
			u.setId(null);
			Optional.ofNullable(u.getEmail()).map(String::trim).ifPresent(u::setEmail);
			Optional.ofNullable(u.getFirstName()).map(String::trim).ifPresent(u::setFirstName);
			Optional.ofNullable(u.getLastName()).map(String::trim).ifPresent(u::setLastName);
		});

		return t;
	}

	private void assertNoDuplicateEmailsInRequest(Collection<Teacher> teachers) {
		var dup = teachers.stream()
				.map(Teacher::getUser)
				.filter(Objects::nonNull)
				.map(AppUser::getEmail)
				.filter(Objects::nonNull)
				.map(String::trim)
				.map(s -> s.toLowerCase())
				.collect(Collectors.groupingBy(s -> s, Collectors.counting()))
				.entrySet()
				.stream()
				.filter(e -> e.getValue() > 1)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());

		if (!dup.isEmpty()) {
			log.error("createTeachers: duplicate emails in request: {}", dup);
			throw new IllegalArgumentException("Duplicate emails in request: " + dup);
		}
	}

	private void assertTeacherEmailsFreeInDb(Collection<Teacher> teachers) {
		var normalized = teachers.stream()
				.map(Teacher::getUser)
				.filter(Objects::nonNull)
				.map(AppUser::getEmail)
				.filter(Objects::nonNull)
				.map(String::trim)
				.map(s -> s.toLowerCase())
				.collect(Collectors.toSet());

		if (normalized.isEmpty()) {
			return;
		}

		var conflicts = usersRepository.findExistingEmailsIgnoreCase(normalized);

		if (!conflicts.isEmpty()) {
			log.warn("createTeachers: emails already exist in DB: {}", conflicts);
			throw new IllegalArgumentException("Emails already exist: " + conflicts);
		}
	}

	private void assertIdsNotNull(Map<Long, ?> dataWithTeacherIdKey) {
		if (dataWithTeacherIdKey.keySet().stream().anyMatch(Objects::isNull)) {
			log.error("updateOffices: map contains null key");
			throw new IllegalArgumentException("teacherId must not be null");
		}
	}

	private void assertAcademicRanksNotNull(Map<Long, AcademicRank> rankByTeacherId) {
		if (rankByTeacherId.values().stream().anyMatch(Objects::isNull)) {
			log.error("updateCodes: map contains null code");
			throw new IllegalArgumentException("course code must not be null");
		}
	}

	private Map<Long, String> normalizeOffices(Map<Long, String> officeByTeacherId) {
		return officeByTeacherId.entrySet()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey,
						e -> Optional.ofNullable(e.getValue()).map(String::trim).orElse(null)));
	}
}
