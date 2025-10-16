package ua.foxminded.university.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.domain.validation.EntityValidatior;
import ua.foxminded.university.model.repository.TeacherRepository;
import ua.foxminded.university.security.PasswordPolicy;
import ua.foxminded.university.service.dto.DeleteResult;
import jakarta.persistence.EntityNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherService {

	private static final Logger log = LoggerFactory.getLogger(TeacherService.class);

	private final TeacherRepository teacherRepository;
	private final EntityValidatior validator;
	private final PasswordPolicy passwordPolicy;

	@Transactional(value = TxType.REQUIRES_NEW)
	public List<Teacher> createAll(Collection<Teacher> teachers) {
		if (teachers == null || teachers.isEmpty()) {
			log.warn("createAll called with null/empty list");
			return List.of();
		}

		var teachersToPersist = teachers.stream().filter(Objects::nonNull).peek(t -> {
			t.setId(null);
			if (t.getUser() != null) {
				t.getUser().setId(null);
			}
		}).toList();

		validator.validateAll(teachersToPersist);
		teachersToPersist.forEach(t -> passwordPolicy.encodeNewPassword(t.getUser()));

		return teacherRepository.saveAll(teachersToPersist);
	}

	@Transactional(value = TxType.SUPPORTS)
	public List<Teacher> findByIds(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			log.warn("findByIds called with null/empty list");
			return List.of();
		}
		var filtered = ids.stream().filter(Objects::nonNull).distinct().toList();

		return teacherRepository.findAllById(filtered);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public int updateOffices(Map<Long, String> officeByTeacherId) {
		if (officeByTeacherId == null || officeByTeacherId.isEmpty()) {
			log.warn("updateOffices: empty/null map");
			return 0;
		}
		assertIdsNotNull(officeByTeacherId);

		var normalized = normalizeOffices(officeByTeacherId);

		validator.validateTeachersOffices(List.copyOf(normalized.values()));

		var ids = List.copyOf(normalized.keySet());
		var existing = teacherRepository.findAllById(ids);

		var found = existing.stream().map(Teacher::getId).collect(Collectors.toSet());
		var missing = ids.stream().filter(id -> !found.contains(id)).toList();
		if (!missing.isEmpty()) {
			log.error("updateOffices: missing teacher ids {}", missing);
			throw new EntityNotFoundException("Teachers not found: " + missing);
		}

		existing.forEach(t -> t.setOffice(normalized.get(t.getId())));

		log.info("updateOffices: updated {}", existing.size());
		return existing.size();
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public int updateAcademicRanks(Map<Long, AcademicRank> rankByTeacherId) {
		if (rankByTeacherId == null || rankByTeacherId.isEmpty()) {
			log.warn("updateAcademicRanks: empty/null map");
			return 0;
		}

		assertIdsNotNull(rankByTeacherId);
		assertAcademicRanksNotNull(rankByTeacherId);

		var ids = List.copyOf(rankByTeacherId.keySet());
		var existing = teacherRepository.findAllById(ids);

		var found = existing.stream().map(Teacher::getId).collect(Collectors.toSet());
		var missing = ids.stream().filter(id -> !found.contains(id)).toList();
		if (!missing.isEmpty()) {
			log.error("updateAcademicRanks: missing teacher ids {}", missing);
			throw new EntityNotFoundException("Teachers not found: " + missing);
		}

		existing.forEach(t -> t.setAcademicRank(rankByTeacherId.get(t.getId())));
		log.info("updateAcademicRanks: updated {}", existing.size());
		return existing.size();
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public DeleteResult deleteByIds(Collection<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			log.warn("deleteByIds called with null/empty list");
			return new DeleteResult(List.of(), List.of());
		}

		var distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
		var existing = teacherRepository.findAllById(distinct);

		var deletedIds = existing.stream().map(Teacher::getId).toList();
		var deletedSet = new HashSet<>(deletedIds);

		var notFound = distinct.stream().filter(id -> !deletedSet.contains(id)).toList();

		teacherRepository.deleteAll(existing);
		log.info("Deleted {} teacher(s); not found: {}", deletedIds.size(), notFound);

		return new DeleteResult(deletedIds, notFound);
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
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() == null ? null : e.getValue().trim()));
	}
}
