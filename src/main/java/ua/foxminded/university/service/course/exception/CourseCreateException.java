package ua.foxminded.university.service.course.exception;

import lombok.Getter;
import ua.foxminded.university.service.course.dto.CourseCreateDto;

@Getter
public class CourseCreateException extends RuntimeException {

    private final CourseCreateDto form;

    public CourseCreateException(CourseCreateDto form, String message) {
        super(message);
        this.form = form;
    }
}