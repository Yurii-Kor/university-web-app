package ua.foxminded.university.service.student.exception;

import lombok.Getter;

@Getter
public class StudentUpdateException extends RuntimeException {

    private final long studentId;

    public StudentUpdateException(
            long studentId,
            String message) {

        super(message);
        this.studentId = studentId;
    }
}
