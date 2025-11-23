package ua.foxminded.university.service;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.repository.StudyGroupRepository;
import ua.foxminded.university.model.repository.dto.IdCountAgg;
import ua.foxminded.university.service.dto.request.StudyGroupDto;
import ua.foxminded.university.service.dto.response.DeleteResult;
import ua.foxminded.university.service.util.RequestDtoNormalizer;
import ua.foxminded.university.service.util.validation.EntityValidatior;
import ua.foxminded.university.service.util.validation.groups.OnCreate;
import ua.foxminded.university.service.util.validation.groups.OnRename;
import ua.foxminded.university.service.util.DtoMapper;
import ua.foxminded.university.service.util.DuplicateGuard;

@Service
@RequiredArgsConstructor
@Transactional
public class StudyGroupService {

	private static final Logger log = LoggerFactory.getLogger(StudyGroupService.class);

	private final StudyGroupRepository groupRepository;
	private final EntityValidatior validator;
	private final RequestDtoNormalizer normalizer;
	private final DtoMapper dtoMapper;
	private final DuplicateGuard duplicateGuard;

	@Transactional(value = TxType.REQUIRES_NEW)
	public List<StudyGroup> createAll(Collection<StudyGroupDto> drafts) {
		var normalized = normalizer.normalizeGroups(drafts);
		validator.validateAll(normalized, OnCreate.class);

		var toPersist = dtoMapper.mapGroupsToEntities(normalized);
		if (toPersist.isEmpty()) {
			log.warn("createAll: nothing to persist (null/empty input)");
			return List.of();
		}

		var namesLower = toPersist.stream()
				.map(StudyGroup::getName)
				.map(String::trim)
				.map(String::toLowerCase)
				.toList();

		duplicateGuard.assertNoDuplicates(namesLower, "normalized study group names");
		assertNamesFreeInDbForCreate(namesLower);

		return groupRepository.saveAll(toPersist);
	}

	@Transactional(value = TxType.SUPPORTS)
	public List<StudyGroup> findByIds(Collection<Long> ids) {
		var distinct = Optional.ofNullable(ids)
				.orElseGet(Collections::emptyList)
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
	public StudyGroup rename(StudyGroupDto patch) {
		var normalized = normalizer.normalizeGroup(patch)
				.orElseThrow(() -> new IllegalArgumentException("patch must not be null"));
		validator.validateAll(List.of(normalized), OnRename.class);

		var managed = groupRepository.findById(normalized.id()).orElseThrow(() -> {
			log.error("rename: StudyGroup not found: id={}", normalized.id());
			return new EntityNotFoundException("StudyGroup not found: id=" + normalized.id());
		});

		Optional.ofNullable(normalized.name())
				.filter(n -> !n.isEmpty())
				.filter(n -> !n.equalsIgnoreCase(managed.getName()))
				.ifPresent(n -> {
					assertNameFreeInDbForRename(n, managed.getId());
					managed.setName(n);
				});

		log.debug("rename: updated name for groupId={}", managed.getId());
		return managed;
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

			log.error("deleteByIds: groups not empty: {}", pairs);
			throw new IllegalStateException("Cannot delete non-empty groups: " + pairs);
		}
	}

	private void assertNamesFreeInDbForCreate(Collection<String> namesLower) {
		var conflicts = groupRepository.findExistingNamesIgnoreCase(new HashSet<>(namesLower));
		if (!conflicts.isEmpty()) {
			log.warn("group names already exist in DB: {}", conflicts);
			throw new IllegalArgumentException("Group names already exist: " + conflicts);
		}
	}

	private void assertNameFreeInDbForRename(String newName, Long selfId) {
		var probe = Set.of(newName.trim().toLowerCase());
		var self = List.of(selfId);

		var conflicts = groupRepository.findConflictingNamesIgnoreCase(probe, self);
		if (!conflicts.isEmpty()) {
			log.warn("target group name already taken by other groups: {}", conflicts);
			throw new IllegalArgumentException("Group names already exist: " + conflicts);
		}
	}
}
