package ua.foxminded.university.service.exception.appuser;

import lombok.Getter;
import ua.foxminded.university.service.dto.request.appuser.AppUserCreateDto;

@Getter
public class AdminCreateException extends RuntimeException {

    private final AppUserCreateDto form;

    public AdminCreateException(AppUserCreateDto form, String message) {
        super(message);
        this.form = form;
    }
}