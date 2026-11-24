package ua.foxminded.university.service.student.exception;

import lombok.Getter;
import ua.foxminded.university.service.student.dto.StudentCreateDto;

@Getter
public class StudentCreateException extends RuntimeException {

    private final String field;
    private final StudentCreateDto form;

    public StudentCreateException(
            String field,
            StudentCreateDto form,
            String message) {

        super(message);
        this.field = field;
        this.form = form;
    }
}