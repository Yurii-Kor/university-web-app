package ua.foxminded.university.web.account.delete.strategy;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.TeacherService;

@Component
@RequiredArgsConstructor
public class TeacherAccountDeleter implements AccountDeleter {

    private final TeacherService teacherService;

    @Override
    public UserRole role() {
        return UserRole.TEACHER;
    }

    @Override
    public void deleteById(long id) {
        teacherService.deleteById(id);
    }
}
