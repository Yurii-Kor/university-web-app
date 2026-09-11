package ua.foxminded.university.service.course.exception;

import java.util.List;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class CourseTeacherReassignmentException
        extends RuntimeException {

    private static final String ERROR_CODE =
            "COURSE_TEACHER_REASSIGNMENT_CONFLICT";

    private final Long courseId;
    private final String courseCode;
    private final String courseName;
    private final TeacherSummary currentTeacher;
    private final TeacherSummary targetTeacher;
    private final List<LessonTeacherReassignmentConflict> conflicts;

    public CourseTeacherReassignmentException(
            Long courseId,
            String courseCode,
            String courseName,
            TeacherSummary currentTeacher,
            TeacherSummary targetTeacher,
            List<LessonTeacherReassignmentConflict> conflicts) {

        super(buildMessage(
                courseId,
                targetTeacher.id(),
                conflicts.size()));

        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.currentTeacher = currentTeacher;
        this.targetTeacher = targetTeacher;
        this.conflicts = List.copyOf(conflicts);
    }

    public String errorCode() {
        return ERROR_CODE;
    }

    private static String buildMessage(
            Long courseId,
            Long targetTeacherId,
            int conflictsCount) {

        return "Cannot reassign course teacher: courseId=%d, targetTeacherId=%d, conflictingLessons=%d"
                .formatted(
                        courseId,
                        targetTeacherId,
                        conflictsCount);
    }
}