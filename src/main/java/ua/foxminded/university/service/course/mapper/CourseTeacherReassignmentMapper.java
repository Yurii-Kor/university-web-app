package ua.foxminded.university.service.course.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import ua.foxminded.university.model.domain.Lesson;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.persistence.lesson.projection.OverlapHit;
import ua.foxminded.university.service.course.exception.LessonTeacherReassignmentConflict;
import ua.foxminded.university.service.course.exception.TeacherSummary;

@Component
public class CourseTeacherReassignmentMapper {

    public TeacherSummary toTeacherSummary(Teacher teacher) {
        var user = teacher.getUser();

        return new TeacherSummary(
                teacher.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail());
    }

    public LessonTeacherReassignmentConflict toConflict(
            Lesson lesson,
            List<OverlapHit> teacherConflicts) {

        return new LessonTeacherReassignmentConflict(
                lesson.getId(),
                lesson.getGroup().getId(),
                lesson.getStartTime(),
                lesson.getEndTime(),
                lesson.getRoom(),
                List.copyOf(teacherConflicts));
    }
}