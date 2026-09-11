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
import ua.foxminded.university.service.lesson.exception.RequestedSlot;
import ua.foxminded.university.service.lesson.exception.ScheduleConflictException;

@Component
@RequiredArgsConstructor
public class LessonConflictChecker {

    private static final Logger log = LoggerFactory.getLogger(LessonConflictChecker.class);

    private final LessonRepository lessonRepository;

    public void assertNoConflicts(Lesson lesson, long teacherId) {
        assertNoConflicts(lesson, teacherId, null);
    }

    public void assertNoConflicts(
            Lesson lesson,
            long teacherId,
            Long excludedLessonId) {

        var startTime = lesson.getStartTime();
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
                excludeLesson(
                        lessonRepository.findTeacherOverlaps(
                                teacherId,
                                startTime,
                                endTime),
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

        throw new ScheduleConflictException(
                RequestedSlot.from(
                        lesson,
                        teacherId),
                groupConflicts,
                teacherConflicts,
                roomConflicts);
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