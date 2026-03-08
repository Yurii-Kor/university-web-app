package ua.foxminded.university.service.exception.course;

import lombok.Getter;
import ua.foxminded.university.service.dto.request.course.CourseSelfUpdateDto;

@Getter
public class CourseSelfUpdateException extends RuntimeException {

    private final CourseSelfUpdateDto form;

    public CourseSelfUpdateException(CourseSelfUpdateDto form, String message) {
        super(message);
        this.form = form;
    }
}