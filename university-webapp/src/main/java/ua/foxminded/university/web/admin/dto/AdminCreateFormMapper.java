package ua.foxminded.university.web.admin.dto;

import org.springframework.stereotype.Component;

import ua.foxminded.university.service.dto.request.appuser.AppUserCreateDto;

@Component
public class AdminCreateFormMapper {

    public AppUserCreateDto toCreateDto(AdminCreateForm form) {
        return new AppUserCreateDto(
                form.email(),
                form.newPassword(),
                form.firstName(),
                form.lastName()
        );
    }
}