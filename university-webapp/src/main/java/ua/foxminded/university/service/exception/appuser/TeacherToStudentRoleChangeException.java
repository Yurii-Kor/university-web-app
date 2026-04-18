package ua.foxminded.university.service.exception.appuser;

import lombok.Getter;
import ua.foxminded.university.service.dto.request.appuser.TeacherToStudentRoleChangeDto;

@Getter
public class TeacherToStudentRoleChangeException extends RuntimeException {

    private final TeacherToStudentRoleChangeDto form;

    public TeacherToStudentRoleChangeException(TeacherToStudentRoleChangeDto form, String message) {
        super(message);
        this.form = form;
    }
}