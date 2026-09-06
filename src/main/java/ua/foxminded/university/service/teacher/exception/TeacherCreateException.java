package ua.foxminded.university.service.teacher.exception;

import lombok.Getter;
import ua.foxminded.university.service.teacher.dto.TeacherCreateDto;

@Getter
public class TeacherCreateException extends RuntimeException {

    private final String field;
    private final TeacherCreateDto form;

    public TeacherCreateException(
            String field,
            TeacherCreateDto form,
            String message) {

        super(message);
        this.field = field;
        this.form = form;
    }
}