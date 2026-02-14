package ua.foxminded.university.web.admin.dto;

import java.util.Optional;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.dto.request.appuser.AppUserCreateDto;
import ua.foxminded.university.web.util.DtoFieldNormalizer;

@Component
@RequiredArgsConstructor
public class AdminCreateFormMapper {

    private final DtoFieldNormalizer normalizer;

    public AppUserCreateDto toCreateDto(AdminCreateForm form) {
		form = Optional.ofNullable(form).orElse(new AdminCreateForm("", "", "", "", ""));
		
        return new AppUserCreateDto(
            normalizer.normalizeEmail(form.email()),
            form.newPassword(),
            normalizer.normalizeField(form.firstName()),
            normalizer.normalizeField(form.lastName())
        );
    }
}
