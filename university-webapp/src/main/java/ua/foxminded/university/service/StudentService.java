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
import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.repository.AppUserRepository;
import ua.foxminded.university.model.repository.StudentRepository;
import ua.foxminded.university.model.repository.StudyGroupRepository;
import ua.foxminded.university.model.repository.dto.StudentProfileView;
import ua.foxminded.university.model.repository.dto.DeletedStudentCardProjection;
import ua.foxminded.university.model.repository.dto.StudentCardView;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.service.dto.request.student.StudentCreateDto;
import ua.foxminded.university.service.dto.response.DeletedStudentCardView;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.DtoMapper;
import ua.foxminded.university.service.util.DuplicateGuard;
import ua.foxminded.university.service.util.projectionmapper.ProjectionMapper;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentService {

	private static final int NOT_UPDATED = 0;

	private static final Logger log = LoggerFactory.getLogger(StudentService.class);

	private final StudentRepository studentRepository;
	private final AppUserRepository usersRepository;
	private final StudyGroupRepository groupRepository;

	private final ProjectionMapper<DeletedStudentCardProjection, DeletedStudentCardView> projectionMapper;
	private final DtoMapper dtoMapper;
	private final EntityValidatior validator;
	private final PasswordPolicy passwordPolicy;
	private final DuplicateGuard duplicateGuard;

	@Transactional(value = TxType.REQUIRES_NEW)
	public List<Student> createAll(Collection<StudentCreateDto> drafts) {
		validator.validateAll(drafts);

		var toPersist = dtoMapper.toStudentEntities(drafts);
		if (toPersist.isEmpty()) {
			log.warn("createAll: nothing to persist (null/empty input)");
			return List.of();
		}

		var emailsLower = toPersist.stream()
				.map(Student::getUser)
				.filter(Objects::nonNull)
				.map(AppUser::getEmail)
				.filter(Objects::nonNull)
				.map(String::trim)
				.map(String::toLowerCase)
				.toList();

		duplicateGuard.assertNoDuplicates(emailsLower, "normalized student emails");
		assertStudentEmailsFreeInDb(emailsLower);
		assertStudentGroupsExist(toPersist);

		toPersist.forEach(s -> s.setGroup(groupRepository.getReferenceById(s.getGroup().getId())));
		toPersist.forEach(s -> passwordPolicy.encodeNewPassword(s.getUser()));

		return studentRepository.saveAll(toPersist);
	}

	@Transactional(value = TxType.SUPPORTS)
	public List<Student> findByIds(Collection<Long> ids) {
		var distinct = Optional.ofNullable(ids)
				.orElseGet(Collections::emptyList)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (distinct.isEmpty()) {
			log.warn("findByIds: null/empty input or only nulls after filtering");
			return List.of();
		}

		return studentRepository.findAllById(distinct);
	}
	
	@Transactional(value = TxType.SUPPORTS)
	public Page<StudentCardView> listStudentCardsForAdmin(Pageable pageable) {
	    return studentRepository.findStudentCardsAll(pageable);
	}

    @Transactional(value = TxType.SUPPORTS)
    public Page<DeletedStudentCardView> listDeletedStudentCardsForAdmin(Pageable pageable) {
        return studentRepository.findDeletedStudentCards(pageable).map(projectionMapper::toView);
    }

	@Transactional(value = TxType.SUPPORTS)
	public StudentProfileView getStudentProfileView(long id) {
		return studentRepository.findStudentProfileViewById(id)
				.orElseThrow(() -> new EntityNotFoundException("Student not found: id=" + id));
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public int moveStudentsToGroup(Collection<Long> studentIds, long targetGroupId) {
		if (Optional.ofNullable(studentIds).map(Collection::isEmpty).orElse(true)) {
			log.warn("moveStudentsToGroup called with null/empty student Ids");
			return NOT_UPDATED;
		}

		var newGroup = requireGroupRef(targetGroupId);

		var ids = studentIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
		if (ids.isEmpty()) {
			log.warn("moveStudentsToGroup: only nulls in provided ids");
			return NOT_UPDATED;
		}

		assertStudentsExist(ids);

		return studentRepository.updateGroupByIds(newGroup, ids);
	}
	
	@Transactional(value = TxType.REQUIRES_NEW)
	public void deleteById(long id) {
	    var student = studentRepository.findById(id)
	            .orElseThrow(() -> new EntityNotFoundException("Active student not found: id=" + id));

	    studentRepository.delete(student);
	    log.info("Soft deleted student: id={}", id);
	}

	

	private void assertStudentEmailsFreeInDb(Collection<String> emailsLower) {
		var normalized = emailsLower.stream()
				.filter(Objects::nonNull)
				.map(String::trim)
				.map(String::toLowerCase)
				.collect(Collectors.toSet());

		if (normalized.isEmpty()) return;

		var conflicts = usersRepository.findExistingEmailsIgnoreCase(normalized);
		if (!conflicts.isEmpty()) {
			log.warn("createAll: emails already exist in DB: {}", conflicts);
			throw new IllegalArgumentException("Emails already exist: " + conflicts);
		}
	}

	private StudyGroup requireGroupRef(long groupId) {
		if (!groupRepository.existsById(groupId)) {
			log.error("moveStudentsToGroup: StudyGroup not found: id={}", groupId);
			throw new EntityNotFoundException("StudyGroup not found: id=" + groupId);
		}

		return groupRepository.getReferenceById(groupId);
	}

	private void assertStudentGroupsExist(Collection<Student> students) {
		var groupIds = students.stream()
				.map(Student::getGroup)
				.filter(Objects::nonNull)
				.map(StudyGroup::getId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (groupIds.isEmpty()) return;

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
}
