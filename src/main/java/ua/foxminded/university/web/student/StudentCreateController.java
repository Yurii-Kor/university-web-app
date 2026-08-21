package ua.foxminded.university.web.student;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.student.StudentService;
import ua.foxminded.university.service.student.dto.StudentCreateDto;
import ua.foxminded.university.service.studygroup.StudyGroupService;

@Controller
@RequestMapping("/students")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class StudentCreateController {

    private final StudentService studentService;
    private final StudyGroupService groupService;

    @InitBinder("form")
    void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(
                String.class,
                new StringTrimmerEditor(true));
    }

    @GetMapping("/create")
    public String createStudentPage(
            @ModelAttribute("form") StudentCreateDto form,
            Model model) {

        model.addAttribute(
                "all_groups",
                groupService.listGroupOptions());

        return "students/create";
    }

    @PostMapping("/create")
    public String createStudent(
            @Valid @ModelAttribute("form") StudentCreateDto form,
            RedirectAttributes ra) {

        studentService.create(form);

        ra.addFlashAttribute(
                "ok",
                "Student created.");

        return "redirect:/students";
    }
}