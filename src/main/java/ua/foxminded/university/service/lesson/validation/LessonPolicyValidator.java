package ua.foxminded.university.service.lesson.validation;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.persistence.course.CourseRepository;
import ua.foxminded.university.model.persistence.lesson.LessonRepository;
import ua.foxminded.university.model.persistence.studygroup.StudyGroupRepository;
import ua.foxminded.university.model.persistence.teacher.TeacherRepository;
import ua.foxminded.university.service.lesson.dto.LessonCreateDto;
import ua.foxminded.university.service.lesson.dto.LessonUpdateDto;
import ua.foxminded.university.service.lesson.dto.LessonUpdateValidationData;
import ua.foxminded.university.service.lesson.dto.LessonValidationViolation;

@Component
@RequiredArgsConstructor
@Transactional(TxType.MANDATORY)
public class LessonPolicyValidator {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final StudyGroupRepository groupRepository;
    private final TeacherRepository teacherRepository;
    
    private final Clock clock;

    public List<LessonValidationViolation> validateForCreate(
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

        return Stream.of(
                validateCourseExists(
                        draft.courseId()),
                validateGroupExists(
                        draft.groupId()),
                validateCourseBelongsToTeacher(
                        draft.courseId(),
                        teacherId,
                        "courseId"),
                validateEndAfterStart(
                        startTime,
                        endTime),
                validateLessonIsFuture(
                        startTime,
                        "startTime",
                        "Lesson start time must be in the future."),
                validateGroupAttachedToCourse(
                        draft.courseId(),
                        draft.groupId(),
                        "groupId"),
                validateRoomIsNotTeacherOffice(
                        draft.room()))
                .flatMap(Optional::stream)
                .toList();
    }

    public List<LessonValidationViolation> validateCurrentStateForUpdate(
            LessonUpdateValidationData lesson,
            long teacherId) {

        return Stream.of(
                validateLessonBelongsToTeacher(
                        lesson,
                        teacherId),
                validateLessonIsFuture(
                        lesson.startTime(),
                        null,
                        "Only future lessons can be modified."),
                validateCourseBelongsToTeacher(
                        lesson.courseId(),
                        teacherId,
                        null),
                validateGroupAttachedToCourse(
                        lesson.courseId(),
                        lesson.groupId(),
                        null))
                .flatMap(Optional::stream)
                .toList();
    }

    public List<LessonValidationViolation> validateRequestedStateForUpdate(
            LessonUpdateDto form) {

        var startTime =
                toOffsetDateTime(
                        form.date(),
                        form.startTime());

        var endTime =
                toOffsetDateTime(
                        form.date(),
                        form.endTime());

        return Stream.of(
                validateEndAfterStart(
                        startTime,
                        endTime),
                validateLessonIsFuture(
                        startTime,
                        "startTime",
                        "Lesson start time must be in the future."),
                validateRoomIsNotTeacherOffice(
                        form.room()))
                .flatMap(Optional::stream)
                .toList();
    }

    public List<LessonValidationViolation> validateForDelete(
            long lessonId,
            long teacherId) {

        return Stream.of(
                validateLessonExists(
                        lessonId),
                validateLessonBelongsToTeacherById(
                        lessonId,
                        teacherId),
                validateLessonIsFutureById(
                        lessonId))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<LessonValidationViolation> validateCourseExists(
            Long courseId) {

        if (!courseRepository.existsById(
                courseId)) {

            return Optional.of(
                    new LessonValidationViolation(
                            "courseId",
                            "Course not found: id=%d"
                                    .formatted(
                                            courseId)));
        }

        return Optional.empty();
    }

    private Optional<LessonValidationViolation> validateGroupExists(
            Long groupId) {

        if (!groupRepository.existsById(
                groupId)) {

            return Optional.of(
                    new LessonValidationViolation(
                            "groupId",
                            "Study group not found: id=%d"
                                    .formatted(
                                            groupId)));
        }

        return Optional.empty();
    }

    private Optional<LessonValidationViolation> validateLessonExists(
            long lessonId) {

        if (!lessonRepository.existsById(
                lessonId)) {

            return Optional.of(
                    new LessonValidationViolation(
                            null,
                            "Lesson not found: id=%d"
                                    .formatted(
                                            lessonId)));
        }

        return Optional.empty();
    }

    private Optional<LessonValidationViolation> validateLessonBelongsToTeacher(
            LessonUpdateValidationData lesson,
            long teacherId) {

        if (!Objects.equals(
                lesson.assignedTeacherId(),
                teacherId)) {

            return Optional.of(
                    new LessonValidationViolation(
                            null,
                            "Lesson does not belong to the teacher: lessonId=%d, teacherId=%d"
                                    .formatted(
                                            lesson.lessonId(),
                                            teacherId)));
        }

        return Optional.empty();
    }

    private Optional<LessonValidationViolation> validateLessonBelongsToTeacherById(
            long lessonId,
            long teacherId) {

        if (!lessonRepository.existsByIdAndAssignedTeacher_Id(
                lessonId,
                teacherId)) {

            return Optional.of(
                    new LessonValidationViolation(
                            null,
                            "Lesson does not belong to the teacher: lessonId=%d, teacherId=%d"
                                    .formatted(
                                            lessonId,
                                            teacherId)));
        }

        return Optional.empty();
    }

    private Optional<LessonValidationViolation> validateLessonIsFutureById(
            long lessonId) {

        if (!lessonRepository.existsByIdAndStartTimeAfter(
                lessonId,
                OffsetDateTime.now(clock))) {

            return Optional.of(
                    new LessonValidationViolation(
                            null,
                            "Only future lessons can be modified."));
        }

        return Optional.empty();
    }

    private Optional<LessonValidationViolation> validateCourseBelongsToTeacher(
            Long courseId,
            long teacherId,
            String field) {

        if (!courseRepository.existsByIdAndTeacher_Id(
                courseId,
                teacherId)) {

            return Optional.of(
                    new LessonValidationViolation(
                            field,
                            "Course does not belong to the teacher: courseId=%d, teacherId=%d"
                                    .formatted(
                                            courseId,
                                            teacherId)));
        }

        return Optional.empty();
    }

    private Optional<LessonValidationViolation> validateGroupAttachedToCourse(
            Long courseId,
            Long groupId,
            String field) {

        if (!courseRepository.existsByIdAndGroups_Id(
                courseId,
                groupId)) {

            return Optional.of(
                    new LessonValidationViolation(
                            field,
                            "Study group is not assigned to the course: groupId=%d, courseId=%d"
                                    .formatted(
                                            groupId,
                                            courseId)));
        }

        return Optional.empty();
    }

    private Optional<LessonValidationViolation> validateEndAfterStart(
            OffsetDateTime startTime,
            OffsetDateTime endTime) {

        if (!endTime.isAfter(
                startTime)) {

            return Optional.of(
                    new LessonValidationViolation(
                            "endTime",
                            "Lesson end time must be after start time."));
        }

        return Optional.empty();
    }

    private Optional<LessonValidationViolation> validateLessonIsFuture(
            OffsetDateTime startTime,
            String field,
            String message) {

        if (!startTime.isAfter(
                OffsetDateTime.now(clock))) {

            return Optional.of(
                    new LessonValidationViolation(
                            field,
                            message));
        }

        return Optional.empty();
    }

    private Optional<LessonValidationViolation> validateRoomIsNotTeacherOffice(
            String roomValue) {

        var room =
                Optional.ofNullable(
                        roomValue)
                        .filter(value ->
                                !value.isBlank());

        if (room.isEmpty()) {
            return Optional.of(
                    new LessonValidationViolation(
                            "room",
                            "Lesson room must be specified."));
        }

        if (teacherRepository.existsByOfficeIgnoreCase(
                room.get())) {

            return Optional.of(
                    new LessonValidationViolation(
                            "room",
                            "Room is reserved as a teacher office: room=%s"
                                    .formatted(
                                            room.get())));
        }

        return Optional.empty();
    }

    private OffsetDateTime toOffsetDateTime(
            LocalDate date,
            LocalTime time) {

        return date.atTime(time)
                .atZone(clock.getZone())
                .toOffsetDateTime();
    }
}