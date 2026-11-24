package ua.foxminded.university.service.lesson;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.Lesson;
import ua.foxminded.university.model.persistence.course.CourseRepository;
import ua.foxminded.university.model.persistence.lesson.LessonRepository;
import ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView;
import ua.foxminded.university.model.persistence.studygroup.StudyGroupRepository;
import ua.foxminded.university.model.persistence.teacher.TeacherRepository;
import ua.foxminded.university.service.course.exception.TeacherSummary;
import ua.foxminded.university.service.lesson.dto.LessonCreateDto;
import ua.foxminded.university.service.lesson.dto.LessonUpdateDto;
import ua.foxminded.university.service.lesson.dto.LessonUpdateValidationData;
import ua.foxminded.university.service.lesson.validation.LessonValidationFacade;
import ua.foxminded.university.service.util.DtoMapper;

@Service
@RequiredArgsConstructor
@Transactional
public class LessonService {

    private static final Logger log =
            LoggerFactory.getLogger(LessonService.class);

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final StudyGroupRepository groupRepository;
    private final TeacherRepository teacherRepository;

    private final LessonValidationFacade validationFacade;
    private final DtoMapper dtoMapper;
    private final Clock clock;

    @Transactional(value = TxType.REQUIRES_NEW)
    public Lesson create(
            long teacherId,
            LessonCreateDto draft) {

        validationFacade.validateForCreate(
                teacherId,
                draft);

        var course =
                courseRepository.getReferenceById(
                        draft.courseId());

        var group =
                groupRepository.getReferenceById(
                        draft.groupId());

        var lesson =
                dtoMapper.toLessonEntity(
                        draft,
                        course,
                        group);

        return lessonRepository.save(lesson);
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    public Lesson update(
            long teacherId,
            long lessonId,
            LessonUpdateDto form) {

        var lesson =
                lessonRepository.findById(
                        lessonId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Lesson not found: id=%d"
                                                .formatted(
                                                        lessonId)));

        var validationData =
                toUpdateValidationData(
                        lesson);

        validationFacade.validateForUpdate(
                teacherId,
                validationData,
                form);

        dtoMapper.applyLessonUpdate(
                lesson,
                form);

        log.info(
                "Updated lesson: id={}",
                lessonId);

        return lesson;
    }
    
    @Transactional(value = TxType.REQUIRES_NEW)
    public void deleteById(
            long teacherId,
            long lessonId) {

        validationFacade.validateForDelete(
                teacherId,
                lessonId);

        var lesson =
                lessonRepository.getReferenceById(
                        lessonId);

        lessonRepository.delete(
                lesson);

        log.info(
                "Deleted lesson: id={}",
                lessonId);
    }
    
    @Transactional(TxType.MANDATORY)
    public void reassignFutureLessons(
            long courseId,
            String courseCode,
            String courseName,
            TeacherSummary currentTeacher,
            TeacherSummary targetTeacher) {

        validationFacade.validateForReassignment(
                courseId,
                courseCode,
                courseName,
                currentTeacher,
                targetTeacher);

        var targetTeacherReference =
                teacherRepository.getReferenceById(
                        targetTeacher.id());

        var futureLessons =
                lessonRepository.findFutureByCourseId(
                        courseId,
                        OffsetDateTime.now(clock));

        futureLessons.forEach(lesson ->
                lesson.setAssignedTeacher(
                        targetTeacherReference));
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

    @Transactional(value = TxType.SUPPORTS)
    public List<LessonScheduleItemView> listGroupScheduleItems(
            long groupId,
            OffsetDateTime from,
            OffsetDateTime to) {

        return lessonRepository.findGroupScheduleItems(
                groupId,
                from,
                to);
    }

    @Transactional(value = TxType.SUPPORTS)
    public List<LessonScheduleItemView> listTeacherScheduleItems(
            long teacherId,
            OffsetDateTime from,
            OffsetDateTime to) {

        return lessonRepository.findTeacherScheduleItems(
                teacherId,
                from,
                to);
    }

    @Transactional(value = TxType.SUPPORTS)
    public LessonScheduleItemView getTeacherScheduleItem(
            long teacherId,
            long lessonId) {

        return lessonRepository.findTeacherScheduleItem(
                teacherId,
                lessonId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Lesson not found for teacher: lessonId=%d, teacherId=%d"
                                        .formatted(
                                                lessonId,
                                                teacherId)));
    }

    private LessonUpdateValidationData toUpdateValidationData(
            Lesson lesson) {

        return new LessonUpdateValidationData(
                lesson.getId(),
                lesson.getCourse().getId(),
                lesson.getGroup().getId(),
                lesson.getAssignedTeacher().getId(),
                lesson.getStartTime());
    }
}