package ua.foxminded.university.web.course;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.CourseService;
import ua.foxminded.university.service.dto.request.course.CourseDescriptionUpdateDto;
import ua.foxminded.university.service.dto.request.course.CourseSelfUpdateDto;
import ua.foxminded.university.web.bind.TrimToNullUppercaseEditor;
import ua.foxminded.university.web.course.page.CoursesPageModelFactory;

@Controller
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CoursesManagementController {

    private final CourseService courseService;
    private final CoursesPageModelFactory pageFactory;

    @GetMapping
    public String coursesPage(@AuthenticationPrincipal UserDetails principal,
                              @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                              Model model) {

        var page = pageFactory.build(principal, pageNumber);
        model.addAttribute("page", page);

        return "courses/courses";
    }

    @InitBinder
    void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        binder.registerCustomEditor(String.class, "description", new StringTrimmerEditor(false));
        binder.registerCustomEditor(String.class, "code", new TrimToNullUppercaseEditor());
    }

    @PostMapping("/description/update")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public String updateDescription(@ModelAttribute("patch") CourseDescriptionUpdateDto patch,
                                    @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                                    RedirectAttributes ra) {

        courseService.updateDescription(patch);

        ra.addFlashAttribute("ok", "Description updated.");
        ra.addFlashAttribute("courseId", patch.id());

        return "redirect:/courses?page=" + normalizePage(pageNumber);
    }

    @PostMapping("/self/update")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateSelf(@ModelAttribute CourseSelfUpdateDto patch,
                             @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                             RedirectAttributes ra) {

        courseService.updateSelf(patch);

        ra.addFlashAttribute("ok", "Course updated.");
        ra.addFlashAttribute("courseId", patch.id());

        return "redirect:/courses?page=" + normalizePage(pageNumber);
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteCourse(@PathVariable("id") Long id,
                               @RequestParam(name = "page", defaultValue = "0") int pageNumber,
                               RedirectAttributes ra) {

        courseService.delete(id);
        ra.addFlashAttribute("ok", "Course deleted.");

        return "redirect:/courses?page=" + normalizePage(pageNumber);
    }

    private int normalizePage(int pageNumber) {
        return Math.max(pageNumber, 0);
    }
}