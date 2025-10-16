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
import ua.foxminded.university.model.repository.dto.OverlapHit;
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

		Long courseId = normalized.getCourse().getId();
		Long groupId = normalized.getGroup().getId();

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
		if (ids == null || ids.isEmpty()) {
			log.warn("findByIds called with null/empty list");
			return List.of();
		}

		var filtered = ids.stream().filter(Objects::nonNull).distinct().toList();
		return filtered.isEmpty() ? List.of() : scheduleRepository.findAllById(filtered);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public DeleteResult deleteByIds(Collection<Long> ids, Long teacherId) {
		if (ids == null || ids.isEmpty()) {
			log.warn("deleteByIds called with null/empty ids");
			return new DeleteResult(List.of(), List.of());
		}
		if (teacherId == null) {
			log.error("deleteByIds called with null teacherId");
			throw new IllegalArgumentException("teacherId must not be null");
		}

		var distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
		if (distinct.isEmpty()) {
			log.warn("deleteByIds: only nulls in ids -> nothing to do");
			return new DeleteResult(List.of(), List.of());
		}

		var existing = new HashSet<>(scheduleRepository.findExistingIds(distinct));
		var notFound = distinct.stream().filter(id -> !existing.contains(id)).toList();
		if (existing.isEmpty()) {
			log.info("deleteByIds: nothing exists among {}", distinct);
			return new DeleteResult(List.of(), notFound);
		}

		assertScheduleEntriesBelongToTeacher(existing, teacherId);

		scheduleRepository.deleteAllByIdInBatch(existing);
		log.info("Deleted {} schedule entry(ies)", existing.size());

		return new DeleteResult(existing.stream().toList(), notFound);
	}

	private void updateCourse(ScheduleEntry managed, ScheduleEntry patch, Long teacherId) {
		if (patch.getCourse() == null || patch.getCourse().getId() == null) {
			log.debug("updateCourse: no-op (patch.course is null) for entry id={}", managed.getId());
			return;
		}

		var newCourseId = patch.getCourse().getId();
		if (Objects.equals(managed.getCourse().getId(), newCourseId)) {
			log.debug("updateCourse: no-op (same courseId={}) for entry id={}", newCourseId, managed.getId());
			return;
		}

		var oldCourseId = managed.getCourse() != null ? managed.getCourse().getId() : null;

		assertCourseBelongsToTeacher(newCourseId, teacherId);
		managed.setCourse(requireCourseRef(newCourseId));

		log.info("updateCourse: entry id={} course {} -> {}", managed.getId(), oldCourseId, newCourseId);
	}

	private void assertCourseBelongsToTeacher(Long courseId, Long teacherId) {
		boolean ok = courseRepository.existsByIdAndTeacher_Id(courseId, teacherId);
		if (!ok) {
			log.error("teacher {} is not owner of course {}", teacherId, courseId);
			throw new IllegalStateException("Course does not belong to the teacher");
		}
	}

	private void assertScheduleEntriesBelongToTeacher(Collection<Long> existingIds, Long teacherId) {
		var ids = existingIds.stream().filter(Objects::nonNull).distinct().toList();
		var owned = new HashSet<>(scheduleRepository.findOwnedIds(ids, teacherId));
		var notOwned = ids.stream().filter(id -> !owned.contains(id)).toList();

		if (!notOwned.isEmpty()) {
			log.error("deleteByIds: entries do not belong to teacher {}: {}", teacherId, notOwned);
			throw new IllegalStateException("Schedule entries do not belong to the teacher: " + notOwned);
		}
	}

	private void updateGroup(ScheduleEntry managed, ScheduleEntry patch) {
		if (patch.getGroup() == null || patch.getGroup().getId() == null) {
			log.debug("updateGroup: no-op (patch.group is null) for entry id={}", managed.getId());
			return;
		}
		Long newGroupId = patch.getGroup().getId();
		if (Objects.equals(managed.getGroup().getId(), newGroupId)) {
			log.debug("updateGroup: no-op (same groupId={}) for entry id={}", newGroupId, managed.getId());
			return;
		}

		Long oldGroupId = managed.getGroup() != null ? managed.getGroup().getId() : null;

		managed.setGroup(requireGroupRef(newGroupId));
		log.info("updateGroup: entry id={} group {} -> {}", managed.getId(), oldGroupId, newGroupId);
	}

	private void assertGroupAttachedToCourse(ScheduleEntry draft) {
		var ok = courseRepository.existsByIdAndGroups_Id(draft.getCourse().getId(), draft.getGroup().getId());
		if (!ok) {
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
		if (patch.getStartTime() == null && patch.getEndTime() == null) {
			log.debug("updateScheduleEntryTime: no-op (both start/end are null) for entry id={}", managed.getId());
			return;
		}

		var startTime = patch.getStartTime() != null ? patch.getStartTime() : managed.getStartTime();
		var endTime = patch.getEndTime() != null ? patch.getEndTime() : managed.getEndTime();

		if (!endTime.isAfter(startTime)) {
			log.error("updateSelf: endTime {} must be after startTime {}", endTime, startTime);
			throw new IllegalStateException("End time must be strictly after start time");
		}

		managed.setStartTime(startTime);
		managed.setEndTime(endTime);

		log.info("updateScheduleEntryTime: entry id={} time [{} - {}]", managed.getId(), startTime, endTime);
	}

	private void updateRoom(ScheduleEntry managed, ScheduleEntry patch) {
		if (patch.getRoom() == null || patch.getRoom().trim().isEmpty()) {
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
		if (room == null)
			return;
		if (teacherRepository.existsByOfficeIgnoreCase(room)) {
			log.error("room '{}' is a teacher office", room);
			throw new IllegalStateException("Room is reserved as a teacher office");
		}
	}

	private void assertNoOverlaps(ScheduleEntry draft, Long teacherId) {
		var start = draft.getStartTime();
		var end = draft.getEndTime();

		Objects.requireNonNull(start, "startTime must not be null");
		Objects.requireNonNull(end, "endTime must not be null");

		var groupHits = scheduleRepository.findGroupOverlaps(draft.getGroup().getId(), start, end);
		var teacherHits = scheduleRepository.findTeacherOverlaps(teacherId, start, end);
		var roomHits = draft.getRoom() == null ? List.<OverlapHit>of()
				: scheduleRepository.findRoomOverlaps(draft.getRoom(), start, end);

		if (draft.getId() != null) {
			groupHits.removeIf(h -> Objects.equals(h.entryId(), draft.getId()));
			teacherHits.removeIf(h -> Objects.equals(h.entryId(), draft.getId()));
			roomHits.removeIf(h -> Objects.equals(h.entryId(), draft.getId()));
		}

		if (!groupHits.isEmpty() || !teacherHits.isEmpty() || !roomHits.isEmpty()) {
			log.error("Conflicts: group={}, teacher={}, room={}",
					groupHits.size(),
					teacherHits.size(),
					roomHits.size());
			throw new ScheduleConflictException(RequestedSlot.from(draft, teacherId), groupHits, teacherHits, roomHits);
		}
	}

	private void updateLessonType(ScheduleEntry managed, ScheduleEntry patch) {
		if (patch.getLessonType() == null) {
			log.debug("updateLessonType: null in patch -> skip");
			return;
		}

		managed.setLessonType(patch.getLessonType());
		log.info("updateLessonType: {}", patch.getLessonType());
	}

	private void updateDescription(ScheduleEntry managed, ScheduleEntry patch) {
		if (patch.getDescription() == null) {
			log.debug("updateDescription: null in patch -> skip");
			return;
		}
		var d = patch.getDescription().trim();
		managed.setDescription(d.isEmpty() ? null : d);
		log.info("updateDescription: len {}", d.length());
	}

	private void assertCreateArgsNotNull(ScheduleEntry draft, Long teacherId) {
		if (draft == null) {
			log.error("create: draft is null");
			throw new IllegalArgumentException("scheduleEntry must not be null");
		}
		if (teacherId == null) {
			log.error("create: teacherId is null");
			throw new IllegalArgumentException("teacherId must not be null");
		}
	}

	private void assertUpdateSelfArgsNotNull(ScheduleEntry patch, Long teacherId) {
		if (patch == null || patch.getId() == null) {
			log.error("updateSelf: invalid args: patchIsNull={}, id={}",
					patch == null,
					patch == null ? null : patch.getId());
			throw new IllegalArgumentException("scheduleEntry.id must not be null for update");
		}
		if (teacherId == null) {
			log.error("updateSelf: teacherId is null");
			throw new IllegalArgumentException("teacherId must not be null");
		}
	}

	private ScheduleEntry normalizeInputData(ScheduleEntry entry) {
		entry.setId(null);

		if (entry.getRoom() != null) {
			entry.setRoom(entry.getRoom().trim().toUpperCase());
		}
		if (entry.getDescription() != null) {
			var d = entry.getDescription().trim();
			entry.setDescription(d.isEmpty() ? null : d);
		}

		if (entry.getLessonType() == null) {
			entry.setLessonType(LessonType.OTHER);
		}

		return entry;
	}
}
