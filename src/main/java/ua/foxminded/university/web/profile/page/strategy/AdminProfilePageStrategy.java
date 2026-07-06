package ua.foxminded.university.web.profile.page.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.foxminded.university.service.AppUserService;
import ua.foxminded.university.web.profile.page.ProfilePageModel;
import ua.foxminded.university.web.profile.page.ProfilePageStrategy;

@Component
@RequiredArgsConstructor
public class AdminProfilePageStrategy implements ProfilePageStrategy {

    private final AppUserService appUserService;

    @Override public String roleKey() { return "admin"; }

    @Override
    public ProfilePageModel build(long userId) {
        var profile = appUserService.getAdminProfileView(userId);
        
        return new ProfilePageModel("profile/admin", profile);
    }
}