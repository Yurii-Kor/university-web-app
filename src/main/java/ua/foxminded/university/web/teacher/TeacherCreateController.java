package ua.foxminded.university.web.teacher;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.service.teacher.dto.TeacherCreateDto;

@Controller
@RequestMapping("/teachers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class TeacherCreateController {

    private final TeacherService teacherService;

    @InitBinder("form")
    void initBinder(WebDataBinder binder) {

        binder.registerCustomEditor(
                String.class,
                new StringTrimmerEditor(true));
    }

    @GetMapping("/create")
    public String createTeacherPage(
            @ModelAttribute("form") TeacherCreateDto form) {

        return "teachers/create";
    }

    @PostMapping("/create")
    public String createTeacher(
            @Valid @ModelAttribute("form") TeacherCreateDto form,
            RedirectAttributes ra) {

        teacherService.create(form);

        ra.addFlashAttribute("ok", "Teacher created.");

        return "redirect:/teachers";
    }
}