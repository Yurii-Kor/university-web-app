package ua.foxminded.university.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import ua.foxminded.university.model.domain.Course;
import ua.foxminded.university.model.domain.ScheduleEntry;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.model.domain.validation.EntityValidatior;
import ua.foxminded.university.model.domain.validation.OnCreate;
import ua.foxminded.university.model.repository.CourseRepository;
import ua.foxminded.university.model.repository.ScheduleEntryRepository;
import ua.foxminded.university.model.repository.StudyGroupRepository;
import ua.foxminded.university.model.repository.TeacherRepository;
import ua.foxminded.university.service.dto.DeleteResult;
import ua.foxminded.university.service.exception.ScheduleConflictException;
import ua.foxminded.university.service.exception.dto.RequestedSlot;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleEntryService {

	private static final Logger log = LoggerFactory.getLogger(ScheduleEntryService.class);

	private final ScheduleEntryRepository scheduleRepository;
	private final CourseRepository courseRepository;
	private final StudyGroupRepository groupRepository;
	private final TeacherRepository teacherRepository;

	private final EntityValidatior validator;

	@Transactional(value = TxType.REQUIRES_NEW)
	public ScheduleEntry create(ScheduleEntry draft, Long teacherId) {
		assertCreateArgsNotNull(draft, teacherId);

		var normalized = normalizeInputData(draft);
		validator.validateAll(List.of(normalized), OnCreate.class);

		var courseId = normalized.getCourse().getId();
		var groupId = normalized.getGroup().getId();

		normalized.setCourse(requireCourseRef(courseId));
		normalized.setGroup(requireGroupRef(groupId));

		assertCourseBelongsToTeacher(courseId, teacherId);
		assertGroupAttachedToCourse(normalized);

		assertRoomIsNotTeacherOffice(normalized.getRoom());
		assertNoOverlaps(normalized, teacherId);

		return scheduleRepository.save(normalized);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public ScheduleEntry updateSelf(ScheduleEntry patch, Long teacherId) {
		assertUpdateSelfArgsNotNull(patch, teacherId);

		var managed = scheduleRepository.findById(patch.getId()).orElseThrow(() -> {
			log.error("updateSelf: ScheduleEntry not found: id={}", patch.getId());
			return new EntityNotFoundException("ScheduleEntry not found: id=" + patch.getId());
		});

		updateCourse(managed, patch, teacherId);
		updateGroup(managed, patch);
		assertGroupAttachedToCourse(managed);
		updateRoom(managed, patch);
		updateScheduleEntryTime(managed, patch);
		assertNoOverlaps(managed, teacherId);
		updateLessonType(managed, patch);
		updateDescription(managed, patch);

		validator.validateAll(List.of(managed));
		return managed;
	}

	@Transactional(value = TxType.SUPPORTS)
	public List<ScheduleEntry> findByIds(Collection<Long> ids) {
		var distinct = Optional.ofNullable(ids)
				.orElseGet(Collections::emptySet)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (distinct.isEmpty()) {
			log.warn("findByIds: null/empty input or only nulls after filtering");
			return List.of();
		}
		
		return scheduleRepository.findAllById(distinct);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public DeleteResult deleteByIds(Collection<Long> ids, Long teacherId) {
		Optional.ofNullable(teacherId).orElseThrow(() -> {
			log.error("deleteByIds called with null teacherId");
			throw new IllegalArgumentException("teacherId must not be null");
		});

		var distinct = Optional.ofNullable(ids)
				.orElseGet(Collections::emptyList)
				.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		if (distinct.isEmpty()) {
			log.warn("deleteByIds: null/empty ids or only nulls -> nothing to delete");
			return new DeleteResult(Set.of(), Set.of());
		}
		
		return doDeleteByIds(distinct, teacherId);
	}
	
	private DeleteResult doDeleteByIds(Collection<Long> distinctIds, Long teacherId) {
		var existing = new HashSet<>(scheduleRepository.findExistingIds(distinctIds));
		var notFound = distinctIds.stream().filter(id -> !existing.contains(id)).collect(Collectors.toSet());
		if (existing.isEmpty()) {
			log.info("deleteByIds: nothing exists among {}", distinctIds);
			return new DeleteResult(Set.of(), notFound);
		}

		assertScheduleEntriesBelongToTeacher(existing, teacherId);

		scheduleRepository.deleteAllByIdInBatch(existing);
		log.info("Deleted {} schedule entry(ies)", existing.size());

		return new DeleteResult(existing, notFound);
	}

	private void updateCourse(ScheduleEntry managed, ScheduleEntry patch, Long teacherId) {
		Optional<Long> newIdOpt = Optional.ofNullable(patch).map(ScheduleEntry::getCourse).map(Course::getId);
		if (newIdOpt.isEmpty()) {
			log.debug("updateCourse: no-op (patch.course is null) for entry id={}", managed.getId());
			return;
		}
		var newCourseId = newIdOpt.get();

		if (Optional.ofNullable(managed.getCourse())
				.map(Course::getId)
				.map(id -> Objects.equals(id, newCourseId))
				.orElse(false)) {
			
			log.debug("updateCourse: no-op (same courseId={}) for entry id={}", newCourseId, managed.getId());
			return;
		}

		assertCourseBelongsToTeacher(newCourseId, teacherId);
		managed.setCourse(requireCourseRef(newCourseId));

		log.info("updateCourse: entry id={} course -> {}", managed.getId(), newCourseId);
	}


	private void assertCourseBelongsToTeacher(Long courseId, Long teacherId) {
		var isExist = courseRepository.existsByIdAndTeacher_Id(courseId, teacherId);
		if (!isExist) {
			log.error("teacher {} is not owner of course {}", teacherId, courseId);
			throw new IllegalStateException("Course does not belong to the teacher");
		}
	}

	private void assertScheduleEntriesBelongToTeacher(Collection<Long> existingIds, Long teacherId) {
		var ids = existingIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
		var owned = new HashSet<>(scheduleRepository.findOwnedIds(ids, teacherId));
		var notOwned = ids.stream().filter(id -> !owned.contains(id)).collect(Collectors.toSet());

		if (!notOwned.isEmpty()) {
			log.error("deleteByIds: entries do not belong to teacher {}: {}", teacherId, notOwned);
			throw new IllegalStateException("Schedule entries do not belong to the teacher: " + notOwned);
		}
	}

	private void updateGroup(ScheduleEntry managed, ScheduleEntry patch) {
		Optional<Long> newGroupIdOpt = Optional.ofNullable(patch).map(ScheduleEntry::getGroup).map(StudyGroup::getId);
		if (newGroupIdOpt.isEmpty()) {
			log.debug("updateGroup: no-op (patch.group is null) for entry id={}", managed.getId());
			return;
		}
		var newGroupId = newGroupIdOpt.get();
		
		Optional<Long> currentGroupIdOpt = Optional.ofNullable(managed.getGroup()).map(StudyGroup::getId);
		if (currentGroupIdOpt.map(id -> Objects.equals(id, newGroupId)).orElse(false)) {
			log.debug("updateGroup: no-op (same groupId={}) for entry id={}", newGroupId, managed.getId());
			return;
		}

		managed.setGroup(requireGroupRef(newGroupId));
		log.info("updateGroup: entry id={} group -> {}", managed.getId(), newGroupId);
	}

	private void assertGroupAttachedToCourse(ScheduleEntry draft) {
		var isExist = courseRepository.existsByIdAndGroups_Id(draft.getCourse().getId(), draft.getGroup().getId());
		if (!isExist) {
			log.error("group {} is not attached to course {}", draft.getGroup().getId(), draft.getCourse().getId());
			throw new IllegalStateException("Group is not attached to the course");
		}
	}

	private Course requireCourseRef(Long id) {
		return courseRepository.findById(id).orElseThrow(() -> {
			log.error("Course not found: id={}", id);
			return new EntityNotFoundException("Course not found: id=" + id);
		});
	}

	private StudyGroup requireGroupRef(Long id) {
		return groupRepository.findById(id).orElseThrow(() -> {
			log.error("StudyGroup not found: id={}", id);
			return new EntityNotFoundException("StudyGroup not found: id=" + id);
		});
	}

	private void updateScheduleEntryTime(ScheduleEntry managed, ScheduleEntry patch) {
		if (Optional.ofNullable(patch).map(p -> p.getStartTime() == null && p.getEndTime() == null).orElse(true)) {
			log.debug("updateScheduleEntryTime: no-op (patch is null) for entry id={}", managed.getId());
			return;
		}

		var startTime = Optional.ofNullable(patch.getStartTime()).orElseGet(managed::getStartTime);
		var endTime = Optional.ofNullable(patch.getEndTime()).orElseGet(managed::getEndTime);

		Optional.ofNullable(endTime).filter(et -> et.isAfter(startTime)).orElseThrow(() -> {
			log.error("updateSelf: endTime {} must be after startTime {}", endTime, startTime);
			return new IllegalStateException("End time must be strictly after start time");
		});

		managed.setStartTime(startTime);
		managed.setEndTime(endTime);

		log.info("updateScheduleEntryTime: entry id={} time [{} - {}]", managed.getId(), startTime, endTime);
	}

	private void updateRoom(ScheduleEntry managed, ScheduleEntry patch) {
		if (Optional.ofNullable(patch)
				.map(ScheduleEntry::getRoom)
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.isEmpty()) {
			log.debug("updateRoom: no-op (patch.room is blank) for entry id={}", managed.getId());
			return;
		}


		var normalizedRoom = patch.getRoom().trim().toUpperCase();

		validator.validateScheduleEntryRoom(normalizedRoom);
		assertRoomIsNotTeacherOffice(normalizedRoom);

		var old = managed.getRoom();
		if (Objects.equals(old, normalizedRoom)) {
			log.debug("updateRoom: normalized equals current -> no change '{}'", normalizedRoom);
			return;
		}
		
		managed.setRoom(normalizedRoom);
		log.info("updateRoom: entry id={} room {} -> {}", managed.getId(), old, normalizedRoom);
	}

	private void assertRoomIsNotTeacherOffice(String room) {
		if (Optional.ofNullable(room).isEmpty()) return;
		
		if (teacherRepository.existsByOfficeIgnoreCase(room)) {
			log.error("room '{}' is a teacher office", room);
			throw new IllegalStateException("Room is reserved as a teacher office");
		}
	}

	private void assertNoOverlaps(ScheduleEntry draft, Long teacherId) {
		var start = Optional.ofNullable(draft).map(ScheduleEntry::getStartTime).orElseThrow(() -> {
			log.error("assertNoOverlaps: startTime is null");
			return new IllegalArgumentException("startTime must not be null");
		});

		var end = Optional.ofNullable(draft).map(ScheduleEntry::getEndTime).orElseThrow(() -> {
			log.error("assertNoOverlaps: endTime is null");
			return new IllegalArgumentException("endTime must not be null");
		});

		var groupId = Optional.ofNullable(draft.getGroup()).map(StudyGroup::getId).orElseThrow(() -> {
			log.error("assertNoOverlaps: group.id is null");
			return new IllegalArgumentException("group.id must not be null");
		});

		var groupHits = scheduleRepository.findGroupOverlaps(groupId, start, end);
		var teacherHits = scheduleRepository.findTeacherOverlaps(teacherId, start, end);

		var roomHits = Optional.ofNullable(draft.getRoom())
				.map(room -> scheduleRepository.findRoomOverlaps(room, start, end))
				.orElseGet(List::of);

		Optional.ofNullable(draft.getId()).ifPresent(selfId -> {
			groupHits.removeIf(h -> Objects.equals(h.entryId(), selfId));
			teacherHits.removeIf(h -> Objects.equals(h.entryId(), selfId));
			roomHits.removeIf(h -> Objects.equals(h.entryId(), selfId));
		});

		if (!groupHits.isEmpty() || !teacherHits.isEmpty() || !roomHits.isEmpty()) {
			log.error("Conflicts: group={}, teacher={}, room={}",
					groupHits.size(),
					teacherHits.size(),
					roomHits.size());
			throw new ScheduleConflictException(RequestedSlot.from(draft, teacherId), groupHits, teacherHits, roomHits);
		}
	}


	private void updateLessonType(ScheduleEntry managed, ScheduleEntry patch) {
		if (Optional.ofNullable(patch).map(ScheduleEntry::getLessonType).isEmpty()) {
			log.debug("updateLessonType: null in patch -> skip");
			return;
		}

		managed.setLessonType(patch.getLessonType());
		log.info("updateLessonType: {}", patch.getLessonType());
	}

	private void updateDescription(ScheduleEntry managed, ScheduleEntry patch) {
		if (Optional.ofNullable(patch).map(ScheduleEntry::getDescription).isEmpty()) {
			log.debug("updateDescription: null in patch -> skip");
			return;
		}

		var d = patch.getDescription().trim();
		managed.setDescription(d.isEmpty() ? null : d);
		log.info("updateDescription: len {}", d.length());
	}

	private void assertCreateArgsNotNull(ScheduleEntry draft, Long teacherId) {
		Optional.ofNullable(draft).orElseThrow(() -> {
			log.error("create: draft is null");
			return new IllegalArgumentException("scheduleEntry must not be null");
		});

		Optional.ofNullable(teacherId).orElseThrow(() -> {
			log.error("create: teacherId is null");
			return new IllegalArgumentException("teacherId must not be null");
		});
	}

	private void assertUpdateSelfArgsNotNull(ScheduleEntry patch, Long teacherId) {
		Optional.ofNullable(patch).map(ScheduleEntry::getId).orElseThrow(() -> {
			log.error("updateSelf: invalid args: patch or patch.id is null");
			return new IllegalArgumentException("scheduleEntry.id must not be null for update");
		});

		Optional.ofNullable(teacherId).orElseThrow(() -> {
			log.error("updateSelf: teacherId is null");
			return new IllegalArgumentException("teacherId must not be null");
		});
	}

	private ScheduleEntry normalizeInputData(ScheduleEntry entry) {
		entry.setId(null);

		Optional.ofNullable(entry.getRoom())
				.map(String::trim)
				.map(String::toUpperCase)
				.filter(newVal -> !Objects.equals(newVal, entry.getRoom()))
				.ifPresent(entry::setRoom);

		Optional.ofNullable(entry.getDescription())
				.map(String::trim)
				.map(s -> s.isEmpty() ? null : s)
				.filter(newVal -> !Objects.equals(newVal, entry.getDescription()))
				.ifPresent(entry::setDescription);

		entry.setLessonType(Optional.ofNullable(entry.getLessonType()).orElse(LessonType.OTHER));

		return entry;
	}
}
