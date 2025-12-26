package ua.foxminded.university.service;

import java.time.OffsetDateTime;
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
import ua.foxminded.university.model.domain.Lesson;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.model.repository.CourseRepository;
import ua.foxminded.university.model.repository.LessonRepository;
import ua.foxminded.university.model.repository.StudyGroupRepository;
import ua.foxminded.university.model.repository.TeacherRepository;
import ua.foxminded.university.service.dto.request.lesson.LessonCreateDto;
import ua.foxminded.university.service.dto.request.lesson.LessonSelfUpdateDto;
import ua.foxminded.university.service.dto.response.DeleteResult;
import ua.foxminded.university.service.exception.ScheduleConflictException;
import ua.foxminded.university.service.exception.dto.RequestedSlot;
import ua.foxminded.university.service.util.DtoMapper;
import ua.foxminded.university.service.util.validation.EntityValidatior;

@Service
@RequiredArgsConstructor
@Transactional
public class LessonService {

	private static final Logger log = LoggerFactory.getLogger(LessonService.class);

	private final LessonRepository scheduleRepository;
	private final CourseRepository courseRepository;
	private final StudyGroupRepository groupRepository;
	private final TeacherRepository teacherRepository;

	private final EntityValidatior validator;
	private final DtoMapper mapper;

	@Transactional(value = TxType.REQUIRES_NEW)
	public Lesson create(LessonCreateDto  draft) {
		draft = Optional.ofNullable(draft)
                .orElseThrow(() -> new IllegalArgumentException("lesson draft must not be null"));

		validator.validate(draft);

		var entry = mapper.toLessonEntity(draft)
				.orElseThrow(() -> new IllegalArgumentException("Mapper produced null for ScheduleEntry"));

		entry.setId(null);
		entry.setLessonType(Optional.ofNullable(draft.lessonType()).orElse(LessonType.OTHER));
		entry.setCourse(requireCourseRef(draft.courseId()));
		entry.setGroup(requireGroupRef(draft.groupId()));

		assertCourseBelongsToTeacher(draft.courseId(), draft.teacherId());
		assertEndAfterStart(entry);
		assertGroupAttachedToCourse(entry);
		assertRoomIsNotTeacherOffice(entry.getRoom());
		assertNoOverlaps(entry, draft.teacherId());

		return scheduleRepository.save(entry);
	}

	@Transactional(value = TxType.REQUIRES_NEW)
	public Lesson updateSelf(LessonSelfUpdateDto patch) {
		patch = Optional.ofNullable(patch)
                .orElseThrow(() -> new IllegalArgumentException("lesson patch must not be null"));

		validator.validate(patch);

		var teacherId = Objects.requireNonNull(patch.teacherId(), "teacherId must not be null");

		var id = patch.id();
		var managed = scheduleRepository.findById(id).orElseThrow(() -> {
			log.error("updateSelf: ScheduleEntry not found: id={}", id);
			return new EntityNotFoundException("ScheduleEntry not found: id=" + id);
		});

		assertEntryBelongsToTeacher(managed, teacherId);

		Optional.ofNullable(patch.courseId()).ifPresent(courseId -> updateCourse(managed, courseId, teacherId));

		Optional.ofNullable(patch.groupId()).ifPresent(newGroup -> updateGroup(managed, newGroup));

		assertGroupAttachedToCourse(managed);

		Optional.ofNullable(patch.room()).ifPresent(newRoom -> updateRoom(managed, newRoom));

		var startTime = patch.startTime();
		var endTime = patch.endTime();
		Optional.ofNullable(startTime).or(() -> Optional.ofNullable(endTime)).ifPresent(__ -> {
			updateEntryTime(managed, startTime, endTime);
		});

		Optional.ofNullable(patch.lessonType()).ifPresent(newType -> managed.setLessonType(newType));

		Optional.ofNullable(patch.description()).ifPresent(desc -> managed.setDescription(desc.trim().isEmpty() ? null : desc));

		assertNoOverlaps(managed, teacherId);

		return managed;
	}

	@Transactional(value = TxType.SUPPORTS)
	public List<Lesson> findByIds(Collection<Long> ids) {
		var distinct = Optional.ofNullable(ids)
				.orElseGet(Collections::emptyList)
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

	private void assertEntryBelongsToTeacher(Lesson managed, Long teacherId) {
		Long courseId = Optional.ofNullable(managed.getCourse())
				.map(Course::getId)
				.orElseThrow(() -> new IllegalStateException("Managed entry has no course"));
		assertCourseBelongsToTeacher(courseId, teacherId);
	}

	private void updateCourse(Lesson managed, Long newCourseId, Long teacherId) {
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

	private void updateGroup(Lesson managed, Long newGroupId) {
		var currentGroupIdOpt = Optional.ofNullable(managed.getGroup()).map(StudyGroup::getId);

		if (currentGroupIdOpt.map(id -> Objects.equals(id, newGroupId)).orElse(false)) {
			log.debug("updateGroup: no-op (same groupId={}) for entry id={}", newGroupId, managed.getId());
			return;
		}

		managed.setGroup(requireGroupRef(newGroupId));
		log.info("updateGroup: entry id={} group -> {}", managed.getId(), newGroupId);
	}

	private void assertGroupAttachedToCourse(Lesson draft) {
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

	private void updateEntryTime(Lesson managed, OffsetDateTime start, OffsetDateTime end) {
		Optional.ofNullable(start).ifPresent(newStart -> managed.setStartTime(newStart));
		Optional.ofNullable(end).ifPresent(newEnd -> managed.setEndTime(newEnd));

		assertEndAfterStart(managed);
		log.info("updateScheduleEntryTime: entry id={} time [{} - {}]",
				managed.getId(),
				managed.getStartTime(),
				managed.getEndTime());
	}

	private void assertNoOverlaps(Lesson draft, Long teacherId) {
		var start = Optional.ofNullable(draft).map(Lesson::getStartTime).orElseThrow(() -> {
			log.error("assertNoOverlaps: startTime is null");
			return new IllegalArgumentException("startTime must not be null");
		});

		var end = Optional.ofNullable(draft).map(Lesson::getEndTime).orElseThrow(() -> {
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

	private void updateRoom(Lesson managed, String patchRoom) {
		var old = managed.getRoom();
		if (Objects.equals(old, patchRoom)) {
			log.debug("updateRoom: normalized equals current -> no change '{}'", patchRoom);
			return;
		}

		assertRoomIsNotTeacherOffice(patchRoom);

		managed.setRoom(patchRoom);
		log.info("updateRoom: entry id={} room {} -> {}", managed.getId(), old, patchRoom);
	}

	private void assertRoomIsNotTeacherOffice(String room) {
		if (teacherRepository.existsByOfficeIgnoreCase(room)) {
			log.error("room '{}' is a teacher office", room);
			throw new IllegalStateException("Room is reserved as a teacher office");
		}
	}

	private void assertEndAfterStart(Lesson e) {
		var st = e.getStartTime();
		var et = e.getEndTime();

		if (!et.isAfter(st)) {
			log.error("time range check failed: endTime {} is not after startTime {} (id={})", et, st, e.getId());
			throw new IllegalStateException("End time must be strictly after start time");
		}
	}
}
