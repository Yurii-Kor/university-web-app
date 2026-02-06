package ua.foxminded.university.web.admin.dto;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.dto.request.appuser.AppUserCreateDto;
import ua.foxminded.university.web.util.DtoFieldNormalizer;

@Component
@RequiredArgsConstructor
public class AdminCreateFormMapper {

    private final DtoFieldNormalizer normalizer;

    public AppUserCreateDto toCreateDto(AdminCreateForm form) {
        return new AppUserCreateDto(
            normalizer.normalizeEmail(form.email()),
            form.newPassword(),
            normalizer.normalizeField(form.firstName()),
            normalizer.normalizeField(form.lastName())
        );
    }
}
