package ua.foxminded.university.web.course;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.foxminded.university.service.CourseService;

import java.util.List;

@Controller
@RequestMapping("/courses/{courseId}/groups")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CourseGroupsController {

    private final CourseService courseService;

    @GetMapping
    public String page(@PathVariable Long courseId,
                       Model model,
                       RedirectAttributes ra) {

        model.addAttribute("pageTitle", "Course groups");

        try {
            model.addAttribute("page", courseService.getCourseGroupsPage(courseId));
            return "courses/course-groups";
        } catch (EntityNotFoundException | IllegalArgumentException | DataAccessException ex) {

            ra.addFlashAttribute("err", safeMessage(ex));
            ra.addFlashAttribute("courseId", courseId);
            ra.addFlashAttribute("courseOp", "groups");
            return "redirect:/courses";
        }
    }

    @PostMapping("/add")
    public String add(@PathVariable Long courseId,
                      @RequestParam("groupId") Long groupId,
                      RedirectAttributes ra) {

        int added = courseService.addGroupsToCourse(courseId, List.of(groupId));
        ra.addFlashAttribute("ok", added > 0 ? "Group added." : "Nothing to add.");
        return "redirect:/courses/" + courseId + "/groups";
    }

    @PostMapping("/remove")
    public String remove(@PathVariable Long courseId,
                         @RequestParam("groupId") Long groupId,
                         RedirectAttributes ra) {

        int removed = courseService.removeGroupsFromCourse(courseId, List.of(groupId));
        ra.addFlashAttribute("ok", removed > 0 ? "Group removed." : "Nothing to remove.");
        return "redirect:/courses/" + courseId + "/groups";
    }
    
    private String safeMessage(Exception ex) {
        String msg = ex.getMessage();
        return (msg == null || msg.isBlank()) ? "Operation failed." : msg;
    }
}
