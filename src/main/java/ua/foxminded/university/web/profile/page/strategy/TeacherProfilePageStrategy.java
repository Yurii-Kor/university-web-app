package ua.foxminded.university.web.profile.page.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.profile.page.ProfilePageModel;
import ua.foxminded.university.web.profile.page.ProfilePageStrategy;

@Component
@RequiredArgsConstructor
public class TeacherProfilePageStrategy implements ProfilePageStrategy {

    private final TeacherService teacherService;

    @Override public String roleKey() { return "teacher"; }

    @Override
    public ProfilePageModel build(long userId) {
        var profile = teacherService.getTeacherProfileView(userId);
        
        return new ProfilePageModel("profile/teacher", profile);
    }
}