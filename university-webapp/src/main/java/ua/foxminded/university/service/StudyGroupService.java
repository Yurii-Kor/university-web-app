package ua.foxminded.university.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.validation.EntityValidatior;
import ua.foxminded.university.model.repository.StudyGroupRepository;
import ua.foxminded.university.model.repository.dto.GroupStudentAgg;
import ua.foxminded.university.service.dto.DeleteResult;

@Service
@RequiredArgsConstructor
@Transactional
public class StudyGroupService {
	
	private final Integer NOT_UPDATED  = 0;

	private static final Logger log = LoggerFactory.getLogger(StudyGroupService.class);

	private final StudyGroupRepository groupRepository;
	private final EntityValidatior validator;

	@Transactional(value = TxType.REQUIRES_NEW)
	public List<StudyGroup> createAll(Collection<StudyGroup> groups) {
		var toPersist = normalizeGroupsToPersist(groups);
		if (toPersist.isEmpty()) {
			log.warn("createAll: nothing to persist (null/empty input or all items null)");
			return List.of();
		}

		var names = toPersist.stream().map(StudyGroup::getName).toList();
		assertNoDuplicatesInRequest(names);
		assertNamesFreeInDb(names);

		validator.validateAll(toPersist);
		return groupRepository.saveAll(toPersist);
	}

	@Transactional(value = TxType.SUPPORTS)
	public List<StudyGroup> findByIds(Collection<Long> ids) {
		var distinct = Optional.ofNullable(ids)
				.orElseGet(Collections::emptySet)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (distinct.isEmpty()) {
			log.warn("findByIds: null/empty input or only nulls after filtering");
			return List.of();
		}
		return groupRepository.findAllById(distinct);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public int rename(Map<Long, String> newNameById) {
		if (Optional.ofNullable(newNameById).map(Map::isEmpty).orElse(true)) {
			log.warn("rename: empty/null map");
			return NOT_UPDATED;
		}

		assertIdsNotNull(newNameById);
		assertStudyGroupNamesNotNull(newNameById);

		var normalized = normalizeGroupNames(newNameById);

		assertNoDuplicatesInRequest(normalized.values());
		assertNamesFreeInDb(normalized);

		validator.validateGroupNames(List.copyOf(normalized.values()));

		var ids = List.copyOf(normalized.keySet());
		var existing = groupRepository.findAllById(ids);

		assertMissingIds(ids, existing);

		existing.forEach(g -> g.setName(normalized.get(g.getId())));
		log.info("rename: updated {}", existing.size());
		return existing.size();
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public DeleteResult deleteByIds(Collection<Long> ids) {
		if (Optional.ofNullable(ids).map(Collection::isEmpty).orElse(true)) {
			log.warn("deleteByIds called with null/empty list");
			return new DeleteResult(Set.of(), Set.of());
		}

		var distinct = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());

		assertGroupsHaveNoStudents(distinct);

		var existing = groupRepository.findAllById(distinct);

		var deletedIds = existing.stream().map(StudyGroup::getId).collect(Collectors.toSet());
		var notFound = distinct.stream().filter(id -> !deletedIds.contains(id)).collect(Collectors.toSet());

		if (existing.isEmpty()) {
			log.info("deleteByIds: nothing to delete; not found: {}", notFound);
			return new DeleteResult(Set.of(), notFound);
		}

		groupRepository.deleteAll(existing);
		log.info("Deleted {} group(s); not found: {}", deletedIds.size(), notFound);

		return new DeleteResult(deletedIds, notFound);
	}

	private void assertGroupsHaveNoStudents(Collection<Long> ids) {
		var aggs = groupRepository.countStudentsByGroupIds(ids);
		var countById = aggs.stream()
				.collect(Collectors.toMap(GroupStudentAgg::groupId, GroupStudentAgg::studentCount));

		var nonEmptyGroups = ids.stream().filter(id -> countById.getOrDefault(id, 0L) > 0L).toList();

		if (!nonEmptyGroups.isEmpty()) {
			var pairs = nonEmptyGroups.stream()
					.collect(Collectors.toMap(id -> id, id -> countById.getOrDefault(id, 0L)));
			log.error("deleteByIds: groups not empty: {}", pairs);
			throw new IllegalStateException("Cannot delete non-empty groups: " + pairs);
		}
	}

	private void assertIdsNotNull(Map<Long, String> newNameById) {
		if (newNameById.keySet().stream().anyMatch(Objects::isNull)) {
			log.warn("rename: map contains null key");
			throw new IllegalArgumentException("groupId must not be null");
		}
	}

	private void assertStudyGroupNamesNotNull(Map<Long, String> newNameById) {
		if (newNameById.values().stream().anyMatch(Objects::isNull)) {
			log.warn("rename: map contains null name");
			throw new IllegalArgumentException("group name must not be null");
		}
	}

	private void assertMissingIds(Collection<Long> allIds, Collection<StudyGroup> existingGroups) {
		var found = existingGroups.stream().map(StudyGroup::getId).collect(Collectors.toSet());
		var missing = allIds.stream().filter(id -> !found.contains(id)).toList();
		if (!missing.isEmpty()) {
			log.warn("missing group ids {}", missing);
			throw new EntityNotFoundException("StudyGroups not found: " + missing);
		}
	}

	private void assertNamesFreeInDb(Map<Long, String> newNameById) {
		var names = newNameById.values().stream().map(String::toLowerCase).collect(Collectors.toSet());
		var ids = List.copyOf(newNameById.keySet());

		var conflicts = groupRepository.findConflictingNamesIgnoreCase(names, ids);
		if (!conflicts.isEmpty()) {
			log.warn("names already taken by other groups: {}", conflicts);
			throw new IllegalArgumentException("Group names already exist: " + conflicts);
		}
	}

	private void assertNamesFreeInDb(List<String> names) {
		var namesLower = names.stream()
				.filter(Objects::nonNull)
				.map(s -> s.trim().toLowerCase())
				.collect(Collectors.toSet());

		var conflicts = groupRepository.findExistingNamesIgnoreCase(namesLower);
		if (!conflicts.isEmpty()) {
			log.warn("group names already exist in DB: {}", conflicts);
			throw new IllegalArgumentException("Group names already exist: " + conflicts);
		}
	}

	private void assertNoDuplicatesInRequest(Collection<String> namesNormalized) {
		var dup = namesNormalized.stream()
				.map(String::toLowerCase)
				.collect(Collectors.groupingBy(n -> n, Collectors.counting()))
				.entrySet()
				.stream()
				.filter(e -> e.getValue() > 1)
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());

		if (!dup.isEmpty()) {
			log.warn("duplicate normalized group names in request: {}", dup);
			throw new IllegalArgumentException("Duplicate normalized group names in request: " + dup);
		}
	}

	private List<StudyGroup> normalizeGroupsToPersist(Collection<StudyGroup> groupsToPersist) {
		return Optional.ofNullable(groupsToPersist).orElseGet(List::of).stream().filter(Objects::nonNull).map(g -> {
			g.setId(null);
			Optional.ofNullable(g.getName()).map(this::normalizeGroupName).ifPresent(g::setName);
			return g;
		}).toList();
	}

	private Map<Long, String> normalizeGroupNames(Map<Long, String> newNameById) {
		return newNameById.entrySet()
				.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> normalizeGroupName(e.getValue())));
	}

	private String normalizeGroupName(String name) {
		var normalized = name.trim().toUpperCase().replaceAll("\\s+", "");
		normalized = normalized.replaceAll("^([A-Z]{2})(\\d{3})$", "$1-$2");

		return normalized;
	}
}
