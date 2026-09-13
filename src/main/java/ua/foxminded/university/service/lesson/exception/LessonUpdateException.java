package ua.foxminded.university.service.lesson.exception;

import java.util.List;

import lombok.Getter;
import ua.foxminded.university.service.lesson.LessonPolicyViolation;
import ua.foxminded.university.service.lesson.dto.LessonUpdateDto;

@Getter
public class LessonUpdateException extends RuntimeException {

    private final long lessonId;
    private final LessonUpdateDto form;
    private final List<LessonPolicyViolation> violations;

    public LessonUpdateException(
            long lessonId,
            LessonUpdateDto form,
            List<LessonPolicyViolation> violations) {

        super("Lesson cannot be updated.");

        this.lessonId = lessonId;
        this.form = form;
        this.violations = List.copyOf(violations);
    }
}