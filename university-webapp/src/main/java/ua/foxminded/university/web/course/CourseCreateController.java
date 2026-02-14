package ua.foxminded.university.web.course;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.repository.dto.TeacherOptionView;
import ua.foxminded.university.service.CourseService;
import ua.foxminded.university.service.TeacherService;
import ua.foxminded.university.web.course.dto.CourseCreateForm;
import ua.foxminded.university.web.course.dto.CourseFormMapper;

@Controller
@RequestMapping("/courses")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CourseCreateController {

	private final CourseService courseService;
	private final TeacherService teacherService;
    private final CourseFormMapper mapper;
    private final Validator validator;

    @GetMapping("/create")
    public String createCoursePage(@ModelAttribute("form") CourseCreateForm form) {
        return "courses/create";
    }
    
    @PostMapping("/create")
    public String createCourse(@ModelAttribute("form") CourseCreateForm form,
                               BindingResult br,
                               RedirectAttributes ra) {

        var dto = mapper.toCreateDto(form);

        validator.validate(dto).forEach(v ->
            br.rejectValue(v.getPropertyPath().toString(), "Invalid", v.getMessage())
        );

        if (br.hasErrors()) {
            return "courses/create";
        }

        courseService.createAll(List.of(dto));

        ra.addFlashAttribute("ok", "Course created.");
        return "redirect:/courses";
    }

    @ModelAttribute("teachers")
    public List<TeacherOptionView> teachers() {
        return teacherService.listTeacherOptions();
    }
}
