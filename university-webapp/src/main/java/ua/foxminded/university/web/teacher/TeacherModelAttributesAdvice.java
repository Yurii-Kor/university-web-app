package ua.foxminded.university.web.teacher;

import java.util.List;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.repository.dto.TeacherOptionView;
import ua.foxminded.university.service.TeacherService;

@ControllerAdvice()
@RequiredArgsConstructor
public class TeacherModelAttributesAdvice {

    private final TeacherService teacherService;

    @ModelAttribute("all_teachers")
    public List<TeacherOptionView> teachers() {
        return teacherService.listTeacherOptions();
    }
}