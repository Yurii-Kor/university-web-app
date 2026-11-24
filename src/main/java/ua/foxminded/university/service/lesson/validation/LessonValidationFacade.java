package ua.foxminded.university.service.lesson.validation;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.course.exception.CourseTeacherReassignmentException;
import ua.foxminded.university.service.course.exception.TeacherSummary;
import ua.foxminded.university.service.lesson.dto.LessonConflictResult;
import ua.foxminded.university.service.lesson.dto.LessonCreateDto;
import ua.foxminded.university.service.lesson.dto.LessonUpdateDto;
import ua.foxminded.university.service.lesson.dto.LessonUpdateValidationData;
import ua.foxminded.university.service.lesson.dto.RequestedSlot;
import ua.foxminded.university.service.lesson.exception.LessonCreateException;
import ua.foxminded.university.service.lesson.exception.LessonUpdateException;
import ua.foxminded.university.service.lesson.exception.ScheduleConflictException;

@Component
@RequiredArgsConstructor
@Transactional(TxType.MANDATORY)
public class LessonValidationFacade {

    private final LessonPolicyValidator policyValidator;
    private final LessonConflictValidator conflictValidator;
    private final LessonTeacherReassignmentValidator reassignmentValidator;

    private final Clock clock;

    public void validateForCreate(
            long teacherId,
            LessonCreateDto draft) {

        var violations =
                policyValidator.validateForCreate(
                        draft,
                        teacherId);

        if (!violations.isEmpty()) {
            throw new LessonCreateException(
                    draft,
                    violations);
        }

        var requested =
                toRequestedSlot(
                        draft,
                        teacherId);

        validateConflicts(requested);
    }

    public void validateForUpdate(
            long teacherId,
            LessonUpdateValidationData lesson,
            LessonUpdateDto form) {

        var violations =
                Stream.concat(
                        policyValidator
                                .validateCurrentStateForUpdate(
                                        lesson,
                                        teacherId)
                                .stream(),
                        policyValidator
                                .validateRequestedStateForUpdate(
                                        form)
                                .stream())
                        .toList();

        if (!violations.isEmpty()) {
            throw new LessonUpdateException(
                    lesson.lessonId(),
                    form,
                    violations);
        }

        var requested =
                toRequestedSlot(
                        lesson,
                        form,
                        teacherId);

        validateConflicts(
                lesson.lessonId(),
                requested);
    }

    public void validateForDelete(
            long teacherId,
            long lessonId) {

        var violations =
                policyValidator.validateForDelete(
                        lessonId,
                        teacherId);

        if (!violations.isEmpty()) {
            throw new IllegalStateException(
                    violations.getFirst()
                            .message());
        }
    }

    public void validateForReassignment(
            long courseId,
            String courseCode,
            String courseName,
            TeacherSummary currentTeacher,
            TeacherSummary targetTeacher) {

        var conflicts =
                reassignmentValidator.findConflicts(
                        courseId,
                        targetTeacher.id());

        if (conflicts.isEmpty()) {
            return;
        }

        throw new CourseTeacherReassignmentException(
                courseId,
                courseCode,
                courseName,
                currentTeacher,
                targetTeacher,
                conflicts);
    }
    
    private RequestedSlot toRequestedSlot(
            LessonCreateDto draft,
            long teacherId) {

        var startTime =
                toOffsetDateTime(
                        draft.date(),
                        draft.startTime());

        var endTime =
                toOffsetDateTime(
                        draft.date(),
                        draft.endTime());

        return new RequestedSlot(
                draft.groupId(),
                draft.courseId(),
                teacherId,
                startTime,
                endTime,
                draft.room(),
                draft.lessonType(),
                draft.description());
    }
    
    private RequestedSlot toRequestedSlot(
            LessonUpdateValidationData lesson,
            LessonUpdateDto form,
            long teacherId) {

        var startTime =
                toOffsetDateTime(
                        form.date(),
                        form.startTime());

        var endTime =
                toOffsetDateTime(
                        form.date(),
                        form.endTime());

        return new RequestedSlot(
                lesson.groupId(),
                lesson.courseId(),
                teacherId,
                startTime,
                endTime,
                form.room(),
                form.lessonType(),
                form.description());
    }
    
    private void validateConflicts(
            RequestedSlot requested) {
        
        validateConflicts(
                null,
                requested);
    }
    
    private void validateConflicts(
            Long lessonId,
            RequestedSlot requested) {

        var conflicts =
                conflictValidator.findConflicts(
                        lessonId,
                        requested.groupId(),
                        requested.teacherId(),
                        requested.startTime(),
                        requested.endTime(),
                        requested.room());

        if (!conflicts.hasConflicts()) {
            return;
        }

        throwScheduleConflict(
                requested,
                conflicts);
    }

    private void throwScheduleConflict(
            RequestedSlot requested,
            LessonConflictResult conflicts) {

        throw new ScheduleConflictException(
                requested,
                conflicts.groupConflicts(),
                conflicts.teacherConflicts(),
                conflicts.roomConflicts());
    }

    private OffsetDateTime toOffsetDateTime(
            LocalDate date,
            LocalTime time) {

        return date.atTime(time)
                .atZone(clock.getZone())
                .toOffsetDateTime();
    }
}