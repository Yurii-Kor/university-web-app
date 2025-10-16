package ua.foxminded.university.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.validation.EntityValidatior;
import ua.foxminded.university.model.domain.validation.OnCreate;
import ua.foxminded.university.model.repository.StudentRepository;
import ua.foxminded.university.model.repository.StudyGroupRepository;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.service.dto.DeleteResult;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentService {

	private static final Logger log = LoggerFactory.getLogger(StudentService.class);

	private final StudentRepository studentRepository;
	private final StudyGroupRepository groupRepository;

	private final EntityValidatior validator;
	private final PasswordPolicy passwordPolicy;

	@Transactional(value = TxType.REQUIRES_NEW)
	public List<Student> createAll(Collection<Student> students) {
		if (students == null || students.isEmpty()) {
			log.warn("createAll called with null/empty list");
			return List.of();
		}

		var toPersist = students.stream().filter(Objects::nonNull).peek(s -> {
			s.setId(null);
			if (s.getUser() != null)
				s.getUser().setId(null);
		}).toList();

		validator.validateAll(toPersist, OnCreate.class);

		assertStudentGroupsExist(getGroupsIds(students));
		toPersist.forEach(s -> s.setGroup(groupRepository.getReferenceById(s.getGroup().getId())));

		toPersist.forEach(s -> passwordPolicy.encodeNewPassword(s.getUser()));

		return studentRepository.saveAll(toPersist);
	}

	@Transactional(value = TxType.SUPPORTS)
	public List<Student> findByIds(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			log.warn("findByIds called with null/empty list");
			return List.of();
		}
		var filtered = ids.stream().filter(Objects::nonNull).distinct().toList();

		return studentRepository.findAllById(filtered);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public Integer moveStudentsToGroup(Collection<Long> studentIds, Long targetGroupId) {
		if (studentIds == null || studentIds.isEmpty()) {
			log.warn("moveStudentsToGroup called with null/empty student Ids");
			return 0;
		}

		var newGroup = requireGroupRef(targetGroupId);

		var ids = studentIds.stream().filter(Objects::nonNull).distinct().toList();
		if (ids.isEmpty()) {
			log.warn("moveStudentsToGroup called with null/empty student Ids");
			return 0;
		}

		assertStudentsExist(ids);

		return studentRepository.updateGroupByIds(newGroup, ids);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public DeleteResult deleteByIds(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			log.warn("deleteByIds called with null/empty list");
			return new DeleteResult(List.of(), List.of());
		}

		var distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
		var existing = studentRepository.findAllById(distinct);

		var deletedIds = existing.stream().map(Student::getId).toList();

		var deletedSet = new HashSet<>(deletedIds);
		var notFound = distinct.stream().filter(id -> !deletedSet.contains(id)).toList();

		studentRepository.deleteAll(existing);
		log.info("Deleted {} student(s); not found: {}", deletedIds.size(), notFound);

		return new DeleteResult(deletedIds, notFound);
	}

	private StudyGroup requireGroupRef(Long groupId) {
		if (groupId == null) {
			log.error("moveStudentsToGroup: null target group id");
			throw new IllegalArgumentException("targetGroupId must not be null");
		}
		if (!groupRepository.existsById(groupId)) {
			log.error("moveStudentsToGroup: StudyGroup not found: id={}", groupId);
			throw new EntityNotFoundException("StudyGroup not found: id=" + groupId);
		}
		return groupRepository.getReferenceById(groupId);
	}

	private void assertStudentGroupsExist(Collection<Long> groupIds) {
		if (groupIds.isEmpty())	return;

		var existing = new HashSet<>(groupRepository.findExistingIds(groupIds));
		var missing = groupIds.stream().filter(id -> !existing.contains(id)).toList();

		if (!missing.isEmpty()) {
			log.error("createAll: student groups not found: {}", missing);
			throw new EntityNotFoundException("StudyGroups not found: " + missing);
		}
	}

	private void assertStudentsExist(Collection<Long> studentIds) {
		if (studentIds.isEmpty()) return;

		var existing = new HashSet<>(studentRepository.findExistingIds(studentIds));
		var missing = studentIds.stream().filter(id -> !existing.contains(id)).toList();

		if (!missing.isEmpty()) {
			log.error("moveStudentsToGroup: some student ids not found: {}", missing);
			throw new EntityNotFoundException("Students not found: " + missing);
		}
	}

	private List<Long> getGroupsIds(Collection<Student> students) {
		return students.stream()
				.filter(Objects::nonNull)
				.map(Student::getGroup)
				.filter(Objects::nonNull)
				.map(StudyGroup::getId)
				.filter(Objects::nonNull)
				.distinct()
				.toList();
	}
}
