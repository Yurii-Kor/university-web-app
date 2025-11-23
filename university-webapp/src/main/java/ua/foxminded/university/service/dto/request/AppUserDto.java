package ua.foxminded.university.service.dto.request;

import jakarta.validation.constraints.*;
import ua.foxminded.university.service.util.validation.groups.OnChangePassword;
import ua.foxminded.university.service.util.validation.groups.OnCreate;
import ua.foxminded.university.service.util.validation.groups.OnUpdateSelf;

public record AppUserDto(
        @NotNull(groups = { OnUpdateSelf.class, OnChangePassword.class })
        Long id,

        @NotBlank(groups = { OnCreate.class })
        @Email(groups = { OnCreate.class, OnUpdateSelf.class })
        @Size(max = 255, groups = { OnCreate.class, OnUpdateSelf.class })
        String email,

        @NotBlank(groups = { OnCreate.class, OnChangePassword.class })
        @Size(min = 8, max = 100, groups = { OnCreate.class, OnChangePassword.class })
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\p{Punct}_])(?=\\S+$).{8,100}$",
            message = "Password must be 8–100 chars, include upper/lowercase, digit, special, and contain no spaces",
            groups = { OnCreate.class, OnChangePassword.class }
        )
        String newPassword,
        
        @NotBlank(groups = OnChangePassword.class)
        String currentPassword,

        @NotBlank(groups = { OnCreate.class })
        @Size(min = 2, max = 64, groups = { OnCreate.class, OnUpdateSelf.class })
        @Pattern(regexp = "^[A-Za-z]+(?:[-'][A-Za-z]+)*$", groups = { OnCreate.class, OnUpdateSelf.class })
        String firstName,

        @NotBlank(groups = { OnCreate.class })
        @Size(min = 2, max = 64, groups = { OnCreate.class, OnUpdateSelf.class })
        @Pattern(regexp = "^[A-Za-z]+(?:[-'][A-Za-z]+)*$", groups = { OnCreate.class, OnUpdateSelf.class })
        String lastName
) {}
