package ua.foxminded.university.service.lesson.exception;

import java.util.List;

import lombok.Getter;
import ua.foxminded.university.service.lesson.dto.LessonCreateDto;
import ua.foxminded.university.service.lesson.dto.LessonValidationViolation;

@Getter
public class LessonCreateException extends RuntimeException {

    private final LessonCreateDto form;
    private final List<LessonValidationViolation> violations;

    public LessonCreateException(
            LessonCreateDto form,
            List<LessonValidationViolation> violations) {

        super("Lesson cannot be created.");

        this.form = form;
        this.violations = List.copyOf(violations);
    }
}