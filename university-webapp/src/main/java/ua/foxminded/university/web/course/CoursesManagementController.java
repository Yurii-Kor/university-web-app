package ua.foxminded.university.web.course;

import java.util.List;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.access.AccessDeniedException;
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
import ua.foxminded.university.web.util.PrincipalHandler;

@Controller
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CoursesManagementController {

    private final CourseService courseService;
    private final PrincipalHandler principalHandler;

    @GetMapping
    public String coursesPage(@AuthenticationPrincipal UserDetails principal, Model model) {
        var roleKey = principalHandler.getRole(principal);
        var userId = principalHandler.parseUserId(principal);

        return switch (roleKey) {

            case "admin" -> {
                model.addAttribute("pageTitle", "Courses");
                model.addAttribute("pageSubtitle", "All courses in the system.");
                model.addAttribute("courses", courseService.listCourseCardsForAdmin());
                yield "courses/courses";
            }

            case "teacher" -> {
                model.addAttribute("pageTitle", "My courses");
                model.addAttribute("pageSubtitle", "Courses you teach.");
                model.addAttribute("courses", courseService.listCourseCardsForTeacher(userId));
                yield "courses/courses";
            }

            case "student" -> {
                model.addAttribute("pageTitle", "My courses");
                model.addAttribute("pageSubtitle", "Courses assigned to your group.");
                model.addAttribute("courses", courseService.listCourseCardsForStudent(userId));
                yield "courses/courses";
            }

            default -> throw new AccessDeniedException("Unsupported role=" + roleKey + " for userId=" + userId);
        };
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
                                    RedirectAttributes ra) {

        courseService.updateDescription(patch);

        ra.addFlashAttribute("ok", "Description updated.");
        ra.addFlashAttribute("courseId", patch.id());
        ra.addFlashAttribute("courseOp", "description");

        return "redirect:/courses";
    }

    @PostMapping("/self/update")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateSelf(@ModelAttribute CourseSelfUpdateDto  patch,
                             RedirectAttributes ra) {

        courseService.updateSelf(patch);

        ra.addFlashAttribute("ok", "Course updated.");
        ra.addFlashAttribute("courseId", patch.id());
        ra.addFlashAttribute("courseOp", "self");

        return "redirect:/courses";
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteCourse(@PathVariable("id") Long id,
                               RedirectAttributes ra) {

        var result = courseService.deleteByIds(List.of(id));

        if (result.deletedIds().contains(id)) {
            ra.addFlashAttribute("ok", "Course deleted.");
        } else if (result.notFoundIds().contains(id)) {
            ra.addFlashAttribute("err", "Course not found.");
        } else {
            ra.addFlashAttribute("ok", "Nothing to delete.");
        }

        ra.addFlashAttribute("courseOp", "delete");
        return "redirect:/courses";
    }
}
