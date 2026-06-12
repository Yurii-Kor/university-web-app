package ua.foxminded.university.web.course;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.foxminded.university.service.CourseService;

@Controller
@RequestMapping("/courses/{courseId}/groups")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class CourseGroupsController {

    private final CourseService courseService;

    @GetMapping
    public String view(@PathVariable long courseId, Model model) {
        model.addAttribute("pageTitle", "Course groups");
        model.addAttribute("page", courseService.getCourseGroupsView(courseId));
        return "courses/course-groups";
    }

    @PostMapping("/add")
    public String add(@PathVariable Long courseId,
                      @RequestParam("groupId") Long groupId,
                      RedirectAttributes ra) {

    	var added = courseService.addGroupToCourse(courseId, groupId);
    	
		ra.addFlashAttribute("ok", added.isEmpty() ? "Nothing to add." : "Group added. ID: " + added.get());
        return "redirect:/courses/" + courseId + "/groups";
    }

    @PostMapping("/remove")
    public String remove(@PathVariable Long courseId,
                         @RequestParam("groupId") Long groupId,
                         RedirectAttributes ra) {

    	var removed = courseService.removeGroupFromCourse(courseId, groupId);
    	
		ra.addFlashAttribute("ok", removed.isEmpty() ? "Nothing to remove." : "Group removed. ID: " + removed.get());
        return "redirect:/courses/" + courseId + "/groups";
    }
}
