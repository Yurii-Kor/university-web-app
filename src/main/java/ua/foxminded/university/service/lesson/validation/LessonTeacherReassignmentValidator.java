package ua.foxminded.university.service.lesson.validation;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.persistence.lesson.LessonRepository;
import ua.foxminded.university.model.persistence.lesson.projection.OverlapHit;
import ua.foxminded.university.service.course.exception.LessonTeacherReassignmentConflict;

@Component
@RequiredArgsConstructor
@Transactional(TxType.MANDATORY)
public class LessonTeacherReassignmentValidator {

    private final LessonRepository lessonRepository;
    private final Clock clock;

    public List<LessonTeacherReassignmentConflict> findConflicts(
            long courseId,
            long targetTeacherId) {

        var futureLessons =
                lessonRepository.findFutureOverlapHitsByCourseId(
                        courseId,
                        OffsetDateTime.now(clock));

        if (futureLessons.isEmpty()) {
            return List.of();
        }

        var rangeStart =
                futureLessons.stream()
                        .map(OverlapHit::startTime)
                        .min(OffsetDateTime::compareTo)
                        .orElseThrow();

        var rangeEnd =
                futureLessons.stream()
                        .map(OverlapHit::endTime)
                        .max(OffsetDateTime::compareTo)
                        .orElseThrow();

        var targetTeacherLessons =
                lessonRepository.findTeacherOverlaps(
                        targetTeacherId,
                        rangeStart,
                        rangeEnd);

        return futureLessons.stream()
                .map(lesson ->
                        findConflict(
                                lesson,
                                targetTeacherLessons))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<LessonTeacherReassignmentConflict> findConflict(
            OverlapHit lesson,
            List<OverlapHit> targetTeacherLessons) {

        var teacherConflicts =
                findTeacherConflicts(
                        lesson,
                        targetTeacherLessons);

        if (teacherConflicts.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                new LessonTeacherReassignmentConflict(
                        lesson.entryId(),
                        lesson.groupId(),
                        lesson.startTime(),
                        lesson.endTime(),
                        lesson.room(),
                        teacherConflicts));
    }

    private List<OverlapHit> findTeacherConflicts(
            OverlapHit lesson,
            List<OverlapHit> targetTeacherLessons) {

        return targetTeacherLessons.stream()
                .filter(candidate ->
                        !Objects.equals(
                                candidate.entryId(),
                                lesson.entryId()))
                .filter(candidate ->
                        overlaps(
                                lesson,
                                candidate))
                .toList();
    }

    private boolean overlaps(
            OverlapHit lesson,
            OverlapHit candidate) {

        return candidate.startTime()
                .isBefore(
                        lesson.endTime())
                && candidate.endTime()
                        .isAfter(
                                lesson.startTime());
    }
}