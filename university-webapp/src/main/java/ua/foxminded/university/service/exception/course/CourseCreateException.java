package ua.foxminded.university.service.exception.course;

import lombok.Getter;
import ua.foxminded.university.service.dto.request.course.CourseCreateDto;

@Getter
public class CourseCreateException extends RuntimeException {

    private final CourseCreateDto form;

    public CourseCreateException(CourseCreateDto form, String message) {
        super(message);
        this.form = form;
    }
}