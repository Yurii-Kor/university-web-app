package ua.foxminded.university.web.profile.page.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.foxminded.university.service.StudentService;
import ua.foxminded.university.web.profile.page.ProfilePageModel;
import ua.foxminded.university.web.profile.page.ProfilePageStrategy;

@Component
@RequiredArgsConstructor
public class StudentProfilePageStrategy implements ProfilePageStrategy {

    private final StudentService studentService;

    @Override public String roleKey() { return "student"; }

    @Override
    public ProfilePageModel build(long userId) {
        var profile = studentService.getStudentProfileView(userId);
        
        return new ProfilePageModel("profile/student", profile);
    }
}