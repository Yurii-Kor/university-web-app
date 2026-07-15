package ua.foxminded.university.service.appuser.exception;

import lombok.Getter;
import ua.foxminded.university.service.appuser.dto.AppUserCreateDto;

@Getter
public class AdminCreateException extends RuntimeException {

    private final AppUserCreateDto form;

    public AdminCreateException(AppUserCreateDto form, String message) {
        super(message);
        this.form = form;
    }
}