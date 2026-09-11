package ua.foxminded.university.service.lesson;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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
import ua.foxminded.university.model.persistence.course.CourseRepository;
import ua.foxminded.university.model.persistence.lesson.LessonRepository;
import ua.foxminded.university.model.persistence.studygroup.StudyGroupRepository;
import ua.foxminded.university.service.lesson.dto.LessonCreateDto;
import ua.foxminded.university.service.lesson.dto.LessonUpdateDto;
import ua.foxminded.university.service.util.DtoMapper;

@Service
@RequiredArgsConstructor
@Transactional
public class LessonService {

    private static final Logger log = LoggerFactory.getLogger(LessonService.class);

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final StudyGroupRepository groupRepository;

    private final LessonPolicy lessonPolicy;
    private final LessonConflictChecker conflictChecker;
    private final DtoMapper dtoMapper;

    @Transactional(value = TxType.REQUIRES_NEW)
    public Lesson create(long teacherId, LessonCreateDto draft) {

        var course = requireCourse(draft.courseId());
        var group = requireGroup(draft.groupId());
        var lesson = createLesson(draft, course, group);

        lessonPolicy.assertCanCreate(lesson, teacherId);
        conflictChecker.assertNoConflicts(lesson, teacherId);

        return lessonRepository.save(lesson);
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public Lesson update(
            long teacherId,
            long lessonId,
            LessonUpdateDto patch) {

        var lesson = requireLesson(lessonId);

        lessonPolicy.assertCanModify(lesson, teacherId);

        applyPatch(lesson, patch);

        lessonPolicy.assertValidAfterUpdate(lesson, teacherId);
        conflictChecker.assertNoConflicts(lesson, teacherId, lessonId);

        log.info("Updated lesson: id={}", lessonId);

        return lesson;
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public void deleteById(long teacherId, long lessonId) {

        var lesson = requireLesson(lessonId);

        lessonPolicy.assertCanModify(lesson, teacherId);

        lessonRepository.delete(lesson);

        log.info("Deleted lesson: id={}", lessonId);
    }

    @Transactional(value = TxType.SUPPORTS)
    public List<Lesson> listGroupSchedule(
            long groupId,
            OffsetDateTime from,
            OffsetDateTime to) {

        return lessonRepository.findGroupSchedule(
                groupId,
                from,
                to);
    }

    @Transactional(value = TxType.SUPPORTS)
    public List<Lesson> listTeacherSchedule(
            long teacherId,
            OffsetDateTime from,
            OffsetDateTime to) {

        return lessonRepository.findTeacherSchedule(
                teacherId,
                from,
                to);
    }

    private Lesson createLesson(
            LessonCreateDto draft,
            Course course,
            StudyGroup group) {

        var lesson = dtoMapper.toLessonEntity(draft)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Cannot map lesson creation data."));

        lesson.setId(null);
        lesson.setCourse(course);
        lesson.setGroup(group);
        lesson.setAssignedTeacher(course.getTeacher());
        lesson.setRoom(normalizeRoom(draft.room()));
        lesson.setLessonType(
                Optional.ofNullable(draft.lessonType())
                        .orElse(LessonType.OTHER));
        lesson.setDescription(
                normalizeDescription(draft.description()));

        return lesson;
    }

    private void applyPatch(
            Lesson lesson,
            LessonUpdateDto patch) {

        Optional.ofNullable(patch.courseId())
                .map(this::requireCourse)
                .ifPresent(lesson::setCourse);

        Optional.ofNullable(patch.groupId())
                .map(this::requireGroup)
                .ifPresent(lesson::setGroup);

        Optional.ofNullable(patch.startTime())
                .ifPresent(lesson::setStartTime);

        Optional.ofNullable(patch.endTime())
                .ifPresent(lesson::setEndTime);

        Optional.ofNullable(patch.room())
                .ifPresent(room ->
                        lesson.setRoom(
                                normalizeRoom(room)));

        Optional.ofNullable(patch.lessonType())
                .ifPresent(lesson::setLessonType);

        Optional.ofNullable(patch.description())
                .ifPresent(description ->
                        lesson.setDescription(
                                normalizeDescription(description)));
    }

    private Course requireCourse(long courseId) {

        return courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Course not found: id=" + courseId));
    }

    private StudyGroup requireGroup(long groupId) {

        return groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "StudyGroup not found: id=" + groupId));
    }

    private Lesson requireLesson(long lessonId) {

        return lessonRepository.findById(lessonId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Lesson not found: id=" + lessonId));
    }

    private String normalizeRoom(String room) {

        return Optional.ofNullable(room)
                .map(String::trim)
                .orElse(null);
    }

    private String normalizeDescription(String description) {

        return Optional.ofNullable(description)
                .map(String::trim)
                .filter(normalized ->
                        !normalized.isEmpty())
                .orElse(null);
    }
}