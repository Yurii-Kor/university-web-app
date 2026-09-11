package ua.foxminded.university.service.lesson;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import org.springframework.stereotype.Component;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.Course;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.persistence.lesson.LessonRepository;
import ua.foxminded.university.service.course.exception.CourseTeacherReassignmentException;
import ua.foxminded.university.service.course.exception.LessonTeacherReassignmentConflict;
import ua.foxminded.university.service.course.mapper.CourseTeacherReassignmentMapper;

@Component
@RequiredArgsConstructor
public class LessonTeacherReassignment {

    private final LessonRepository lessonRepository;
    private final LessonConflictChecker conflictChecker;
    private final CourseTeacherReassignmentMapper mapper;
    private final Clock clock;

    @Transactional(TxType.MANDATORY)
    public void reassignFutureLessons(
            Course course,
            Teacher targetTeacher) {

        var currentTeacher = course.getTeacher();
        var now = OffsetDateTime.now(clock);

        var futureLessons =
                lessonRepository.findFutureByCourseId(
                        course.getId(),
                        now);

        var conflicts =
                new ArrayList<LessonTeacherReassignmentConflict>();

        for (var lesson : futureLessons) {
            var teacherConflicts =
                    conflictChecker.findTeacherConflicts(
                            lesson,
                            targetTeacher.getId(),
                            lesson.getId());

            if (!teacherConflicts.isEmpty()) {
                conflicts.add(
                        mapper.toConflict(
                                lesson,
                                teacherConflicts));
            }
        }

        if (!conflicts.isEmpty()) {
            throw new CourseTeacherReassignmentException(
                    course.getId(),
                    course.getCode(),
                    course.getName(),
                    mapper.toTeacherSummary(
                            currentTeacher),
                    mapper.toTeacherSummary(
                            targetTeacher),
                    conflicts);
        }

        futureLessons.forEach(lesson ->
                lesson.setAssignedTeacher(
                        targetTeacher));
    }
}