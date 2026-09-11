package ua.foxminded.university.service.lesson;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.Course;
import ua.foxminded.university.model.domain.Lesson;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.persistence.course.CourseRepository;
import ua.foxminded.university.model.persistence.teacher.TeacherRepository;

@Component
@RequiredArgsConstructor
public class LessonPolicy {

    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final Clock clock;

    public void assertCanCreate(Lesson lesson, long teacherId) {

        assertCourseBelongsToTeacher(lesson, teacherId);
        assertLessonBelongsToTeacher(lesson, teacherId);
        assertValidScheduledState(lesson);
    }

    public void assertCanModify(Lesson lesson, long teacherId) {

        assertLessonBelongsToTeacher(lesson, teacherId);
        assertLessonIsFuture(lesson, "Only future lessons can be modified.");
    }

    public void assertValidAfterUpdate(Lesson lesson, long teacherId) {

        assertLessonBelongsToTeacher(lesson, teacherId);
        assertCourseBelongsToTeacher(lesson, teacherId);
        assertValidScheduledState(lesson);
    }

    private void assertValidScheduledState(Lesson lesson) {

        assertEndAfterStart(lesson);
        assertLessonIsFuture(lesson, "Lesson start time must be in the future.");
        assertGroupAttachedToCourse(lesson);
        assertRoomIsNotTeacherOffice(lesson);
    }

    private void assertLessonBelongsToTeacher(Lesson lesson, long teacherId) {
        var assignedTeacherId = requireAssignedTeacherId(lesson);

        if (assignedTeacherId != teacherId) {
            
            throw new IllegalStateException("Lesson does not belong to the teacher: lessonId=%d, teacherId=%d"
                    .formatted(lesson.getId(), teacherId));
        }
    }

    private void assertCourseBelongsToTeacher(Lesson lesson, long teacherId) {
        var courseId = requireCourseId(lesson);

        if (!courseRepository.existsByIdAndTeacher_Id(courseId, teacherId)) {

            throw new IllegalStateException(
                    "Course does not belong to the teacher: courseId=%d, teacherId=%d"
                        .formatted(courseId, teacherId));
        }
    }

    private void assertGroupAttachedToCourse(Lesson lesson) {

        var courseId = requireCourseId(lesson);
        var groupId = requireGroupId(lesson);

        if (!courseRepository.existsByIdAndGroups_Id(courseId, groupId)) {

            throw new IllegalStateException(
                    "Study group is not assigned to the course: groupId=%d, courseId=%d"
                        .formatted(groupId, courseId));
        }
    }

    private void assertEndAfterStart(Lesson lesson) {

        var startTime = requireStartTime(lesson);
        var endTime = requireEndTime(lesson);

        if (!endTime.isAfter(startTime)) {
            throw new IllegalStateException("Lesson end time must be after start time.");
        }
    }

    private void assertLessonIsFuture(Lesson lesson, String message) {
        var startTime = requireStartTime(lesson);

        if (!startTime.isAfter(OffsetDateTime.now(clock))) {

            throw new IllegalStateException(message);
        }
    }

    private void assertRoomIsNotTeacherOffice(Lesson lesson) {

        var room = Optional.ofNullable(lesson.getRoom())
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalStateException("Lesson room must be specified."));

        if (teacherRepository.existsByOfficeIgnoreCase(room)) {
            throw new IllegalStateException("Room is reserved as a teacher office: room=" + room);
        }
    }

    private long requireAssignedTeacherId(Lesson lesson) {

        return Optional.ofNullable(lesson.getAssignedTeacher())
                .map(Teacher::getId)
                .orElseThrow(() -> new IllegalStateException("Lesson has no assigned teacher."));
    }

    private long requireCourseId(Lesson lesson) {

        return Optional.ofNullable(lesson.getCourse())
                .map(Course::getId)
                .orElseThrow(() -> new IllegalStateException("Lesson has no course."));
    }

    private long requireGroupId(Lesson lesson) {

        return Optional.ofNullable(lesson.getGroup())
                .map(StudyGroup::getId)
                .orElseThrow(() -> new IllegalStateException("Lesson has no study group."));
    }

    private OffsetDateTime requireStartTime(Lesson lesson) {

        return Optional.ofNullable(lesson.getStartTime())
                .orElseThrow(() -> new IllegalStateException("Lesson start time must be specified."));
    }

    private OffsetDateTime requireEndTime(Lesson lesson) {

        return Optional.ofNullable(lesson.getEndTime())
                .orElseThrow(() -> new IllegalStateException("Lesson end time must be specified."));
    }
}