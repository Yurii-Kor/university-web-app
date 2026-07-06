package ua.foxminded.university.service.exception.course;

import lombok.Getter;

@Getter
public class CourseGroupsOpException extends RuntimeException {
    private final long courseId;

    public CourseGroupsOpException(long courseId, String message) {
        super(message);
        this.courseId = courseId;
    }
}
