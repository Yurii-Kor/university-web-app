package ua.foxminded.university.service.exception.appuser;

import lombok.Getter;
import ua.foxminded.university.service.dto.request.appuser.StudentToTeacherRoleChangeDto;

@Getter
public class StudentToTeacherRoleChangeException extends RuntimeException {

    private final StudentToTeacherRoleChangeDto form;

    public StudentToTeacherRoleChangeException(StudentToTeacherRoleChangeDto form, String message) {
        super(message);
        this.form = form;
    }
}