package ua.foxminded.university.web.course;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.course.CourseService;
import ua.foxminded.university.service.course.dto.CourseCreateDto;
import ua.foxminded.university.web.bind.TrimToNullUppercaseEditor;

@Controller
@RequestMapping("/courses")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CourseCreateController {

	private final CourseService courseService;

    @InitBinder("form")
    void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        binder.registerCustomEditor(String.class, "code", new TrimToNullUppercaseEditor());
    }
    
    @GetMapping("/create")
    public String createCoursePage(@ModelAttribute("form") CourseCreateDto form) {
        return "courses/create";
    }

    @PostMapping("/create")
    public String createCourse(@Valid @ModelAttribute("form") CourseCreateDto form,
                               BindingResult br,
                               RedirectAttributes ra) {
        if (br.hasErrors()) return "courses/create";

        courseService.create(form);
        ra.addFlashAttribute("ok", "Course created.");
        return "redirect:/courses";
    }
}
