package ua.foxminded.university.service.lesson.exception;

import java.util.List;

import lombok.Getter;
import ua.foxminded.university.service.lesson.LessonPolicyViolation;
import ua.foxminded.university.service.lesson.dto.LessonCreateDto;

@Getter
public class LessonCreateException extends RuntimeException {

    private final LessonCreateDto form;
    private final List<LessonPolicyViolation> violations;

    public LessonCreateException(
            LessonCreateDto form,
            List<LessonPolicyViolation> violations) {

        super("Lesson cannot be created.");

        this.form = form;
        this.violations = List.copyOf(violations);
    }
}