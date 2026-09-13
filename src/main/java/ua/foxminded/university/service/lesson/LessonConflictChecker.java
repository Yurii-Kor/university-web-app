package ua.foxminded.university.service.lesson;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.Lesson;
import ua.foxminded.university.model.persistence.lesson.LessonRepository;
import ua.foxminded.university.model.persistence.lesson.projection.OverlapHit;
import ua.foxminded.university.service.lesson.exception.ScheduleConflictException;
import ua.foxminded.university.service.util.DtoMapper;

@Component
@RequiredArgsConstructor
public class LessonConflictChecker {

	private static final Logger log =
			LoggerFactory.getLogger(
					LessonConflictChecker.class);

	private final LessonRepository lessonRepository;
	private final DtoMapper dtoMapper;

	public void assertNoConflicts(
			Lesson lesson,
			long teacherId) {

		assertNoConflicts(
				lesson,
				teacherId,
				null);
	}

	public void assertNoConflicts(
			Lesson lesson,
			long teacherId,
			Long excludedLessonId) {

		var startTime =	lesson.getStartTime();
		var endTime = lesson.getEndTime();
		var groupId = lesson.getGroup().getId();

		var groupConflicts =
				excludeLesson(
						lessonRepository.findGroupOverlaps(
								groupId,
								startTime,
								endTime),
						excludedLessonId);

		var teacherConflicts =
				findTeacherConflicts(
						lesson,
						teacherId,
						excludedLessonId);

		var roomConflicts =
				excludeLesson(
						lessonRepository.findRoomOverlaps(
								lesson.getRoom(),
								startTime,
								endTime),
						excludedLessonId);

		if (groupConflicts.isEmpty()
				&& teacherConflicts.isEmpty()
				&& roomConflicts.isEmpty()) {

			return;
		}

		log.warn(
				"Lesson schedule conflict: group={}, teacher={}, room={}",
				groupConflicts.size(),
				teacherConflicts.size(),
				roomConflicts.size());

		var requested =
				dtoMapper.toRequestedSlot(
						lesson,
						teacherId);

		throw new ScheduleConflictException(
				requested,
				groupConflicts,
				teacherConflicts,
				roomConflicts);
	}

	public List<OverlapHit> findTeacherConflicts(
			Lesson lesson,
			long teacherId,
			Long excludedLessonId) {

		return excludeLesson(
				lessonRepository.findTeacherOverlaps(
						teacherId,
						lesson.getStartTime(),
						lesson.getEndTime()),
				excludedLessonId);
	}

	private List<OverlapHit> excludeLesson(
			List<OverlapHit> conflicts,
			Long excludedLessonId) {

		if (excludedLessonId == null) {
			return conflicts;
		}

		return conflicts.stream()
				.filter(conflict ->
						!Objects.equals(
								conflict.entryId(),
								excludedLessonId))
				.toList();
	}
}