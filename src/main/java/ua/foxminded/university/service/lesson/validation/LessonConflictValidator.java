package ua.foxminded.university.service.lesson.validation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.persistence.lesson.LessonRepository;
import ua.foxminded.university.model.persistence.lesson.projection.OverlapHit;
import ua.foxminded.university.service.lesson.dto.LessonConflictResult;

@Component
@RequiredArgsConstructor
@Transactional(TxType.MANDATORY)
public class LessonConflictValidator {

    private final LessonRepository lessonRepository;

    public LessonConflictResult findConflicts(
            long groupId,
            long teacherId,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String room) {

        return findConflicts(
                null,
                groupId,
                teacherId,
                startTime,
                endTime,
                room);
    }

    public LessonConflictResult findConflicts(
            Long lessonId,
            long groupId,
            long teacherId,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String room) {

        var groupConflicts =
                excludeCurrentLesson(
                        lessonRepository.findGroupOverlaps(
                                groupId,
                                startTime,
                                endTime),
                        lessonId);

        var teacherConflicts =
                excludeCurrentLesson(
                        lessonRepository.findTeacherOverlaps(
                                teacherId,
                                startTime,
                                endTime),
                        lessonId);

        var roomConflicts =
                excludeCurrentLesson(
                        lessonRepository.findRoomOverlaps(
                                room,
                                startTime,
                                endTime),
                        lessonId);

        return new LessonConflictResult(
                groupConflicts,
                teacherConflicts,
                roomConflicts);
    }

    private List<OverlapHit> excludeCurrentLesson(
            List<OverlapHit> conflicts,
            Long lessonId) {

        if (lessonId == null) {
            return conflicts;
        }

        return conflicts.stream()
                .filter(conflict ->
                        !Objects.equals(
                                conflict.entryId(),
                                lessonId))
                .toList();
    }
}