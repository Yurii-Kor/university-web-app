package ua.foxminded.university.service.course.exception;

import lombok.Getter;
import ua.foxminded.university.service.course.dto.CourseSelfUpdateDto;

@Getter
public class CourseSelfUpdateException extends RuntimeException {

    private final CourseSelfUpdateDto form;

    public CourseSelfUpdateException(CourseSelfUpdateDto form, String message) {
        super(message);
        this.form = form;
    }
}