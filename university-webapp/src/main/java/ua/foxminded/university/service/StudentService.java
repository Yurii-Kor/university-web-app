package ua.foxminded.university.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.validation.EntityValidatior;
import ua.foxminded.university.model.domain.validation.OnCreate;
import ua.foxminded.university.model.repository.AppUserRepository;
import ua.foxminded.university.model.repository.StudentRepository;
import ua.foxminded.university.model.repository.StudyGroupRepository;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.service.dto.DeleteResult;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentService {
	
	private final Integer NOT_UPDATED  = 0;

	private static final Logger log = LoggerFactory.getLogger(StudentService.class);

	private final StudentRepository studentRepository;
	private final AppUserRepository usersRepository;
	private final StudyGroupRepository groupRepository;

	private final EntityValidatior validator;
	private final PasswordPolicy passwordPolicy;

	@Transactional(value = TxType.REQUIRES_NEW)
	public List<Student> createAll(Collection<Student> students) {
		var toPersist = normalizeStudentsToPersist(students);
		if (toPersist.isEmpty()) {
			log.warn("createAll: nothing to persist (null/empty input or all items null)");
			return List.of();
		}

		validator.validateAll(toPersist, OnCreate.class);

		assertNoDuplicateEmailsInRequest(students);
		assertStudentEmailsFreeInDb(students);
		assertStudentGroupsExist(getGroupsIds(students));
		toPersist.forEach(s -> s.setGroup(groupRepository.getReferenceById(s.getGroup().getId())));

		toPersist.forEach(s -> passwordPolicy.encodeNewPassword(s.getUser()));

		return studentRepository.saveAll(toPersist);
	}

	@Transactional(value = TxType.SUPPORTS)
	public List<Student> findByIds(Collection<Long> ids) {
		var distinct = Optional.ofNullable(ids)
				.orElseGet(Collections::emptySet)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (distinct.isEmpty()) {
			log.warn("findByIds: null/empty input or only nulls after filtering");
			return List.of();
		}
		
		return studentRepository.findAllById(distinct);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public Integer moveStudentsToGroup(Collection<Long> studentIds, Long targetGroupId) {
		if (Optional.ofNullable(studentIds).map(Collection::isEmpty).orElse(true)) {
			log.warn("moveStudentsToGroup called with null/empty student Ids");
			return NOT_UPDATED;
		}

		var newGroup = requireGroupRef(targetGroupId);

		var ids = studentIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
		if (ids.isEmpty()) {
			log.warn("moveStudentsToGroup called with null/empty student Ids");
			return NOT_UPDATED;
		}

		assertStudentsExist(ids);

		return studentRepository.updateGroupByIds(newGroup, ids);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public DeleteResult deleteByIds(Collection<Long> ids) {
		if (Optional.ofNullable(ids).map(Collection::isEmpty).orElse(true)) {
			log.warn("deleteByIds called with null/empty list");
			return new DeleteResult(Set.of(), Set.of());
		}

		var distinct = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
		var existing = studentRepository.findAllById(distinct);

		var deletedIds = existing.stream().map(Student::getId).collect(Collectors.toSet());

		var notFound = distinct.stream().filter(id -> !deletedIds.contains(id)).collect(Collectors.toSet());

		studentRepository.deleteAll(existing);
		log.info("Deleted {} student(s); not found: {}", deletedIds.size(), notFound);

		return new DeleteResult(deletedIds, notFound);
	}
	
	private List<Student> normalizeStudentsToPersist(Collection<Student> studentsToPersist) {
		return Optional.ofNullable(studentsToPersist)
				.orElseGet(List::of)
				.stream()
				.filter(Objects::nonNull)
				.map(this::normalizeStudentToPersist)
				.toList();
	}

	private Student normalizeStudentToPersist(Student s) {
		s.setId(null);

		Optional.ofNullable(s.getUser()).ifPresent(u -> {
			u.setId(null);
			Optional.ofNullable(u.getEmail()).map(String::trim).ifPresent(u::setEmail);
			Optional.ofNullable(u.getFirstName()).map(String::trim).ifPresent(u::setFirstName);
			Optional.ofNullable(u.getLastName()).map(String::trim).ifPresent(u::setLastName);
		});

		return s;
	}

	private void assertNoDuplicateEmailsInRequest(Collection<Student> students) {
		var dup = students.stream()
				.map(Student::getUser)
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
			log.error("createStudents: duplicate emails in request: {}", dup);
			throw new IllegalArgumentException("Duplicate emails in request: " + dup);
		}
	}

	private void assertStudentEmailsFreeInDb(Collection<Student> students) {
		var normalized = students.stream()
				.map(Student::getUser)
				.filter(Objects::nonNull)
				.map(AppUser::getEmail)
				.filter(Objects::nonNull)
				.map(String::trim)
				.map(s -> s.toLowerCase())
				.collect(Collectors.toSet());

		var conflicts = usersRepository.findExistingEmailsIgnoreCase(normalized);

		if (!conflicts.isEmpty()) {
			log.warn("createStudents: emails already exist in DB: {}", conflicts);
			throw new IllegalArgumentException("Emails already exist: " + conflicts);
		}
	}

	private StudyGroup requireGroupRef(Long groupId) {
		Optional.ofNullable(groupId).orElseThrow(() -> {
			log.error("moveStudentsToGroup: null target group id");
			return new IllegalArgumentException("targetGroupId must not be null");
		});

		Optional.of(groupId).filter(groupRepository::existsById).orElseThrow(() -> {
			log.error("moveStudentsToGroup: StudyGroup not found: id={}", groupId);
			return new EntityNotFoundException("StudyGroup not found: id=" + groupId);
		});

		return groupRepository.getReferenceById(groupId);
	}

	private void assertStudentGroupsExist(Collection<Long> groupIds) {
		if (groupIds.isEmpty())	return;

		var existing = new HashSet<>(groupRepository.findExistingIds(groupIds));
		var missing = groupIds.stream().filter(id -> !existing.contains(id)).collect(Collectors.toSet());

		if (!missing.isEmpty()) {
			log.error("createAll: student groups not found: {}", missing);
			throw new EntityNotFoundException("StudyGroups not found: " + missing);
		}
	}

	private void assertStudentsExist(Collection<Long> studentIds) {
		if (studentIds.isEmpty()) return;

		var existing = new HashSet<>(studentRepository.findExistingIds(studentIds));
		var missing = studentIds.stream().filter(id -> !existing.contains(id)).collect(Collectors.toSet());

		if (!missing.isEmpty()) {
			log.error("moveStudentsToGroup: some student ids not found: {}", missing);
			throw new EntityNotFoundException("Students not found: " + missing);
		}
	}

	private Set<Long> getGroupsIds(Collection<Student> students) {
		return students.stream()
				.filter(Objects::nonNull)
				.map(Student::getGroup)
				.filter(Objects::nonNull)
				.map(StudyGroup::getId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
	}
}
