package ua.foxminded.university.web.account.delete.strategy;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.StudentService;

@Component
@RequiredArgsConstructor
public class StudentAccountDeleter implements AccountDeleter {

    private final StudentService studentService;

    @Override
    public UserRole role() {
        return UserRole.STUDENT;
    }

    @Override
    public void deleteById(long id) {
        studentService.deleteById(id);
    }
}
