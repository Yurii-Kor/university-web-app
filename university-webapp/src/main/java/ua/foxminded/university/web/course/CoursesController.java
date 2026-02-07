package ua.foxminded.university.web.course;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.CourseService;
import ua.foxminded.university.web.util.PrincipalHandler;

@Controller
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CoursesController {

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

    // ---- placeholders (redirect back to list) ----

    @GetMapping("/create")
    public String createCoursePlaceholder(@AuthenticationPrincipal UserDetails principal, RedirectAttributes ra) {
        ra.addFlashAttribute("err", "Create course is not implemented yet.");
        return "redirect:/courses";
    }

    @PostMapping("/{id}/update")
    public String updateCoursePlaceholder(@AuthenticationPrincipal UserDetails principal,
                                         @PathVariable("id") Long id,
                                         RedirectAttributes ra) {
        ra.addFlashAttribute("err", "Update course is not implemented yet.");
        return "redirect:/courses";
    }

    @PostMapping("/{id}/delete")
    public String deleteCoursePlaceholder(@AuthenticationPrincipal UserDetails principal,
                                         @PathVariable("id") Long id,
                                         RedirectAttributes ra) {
        ra.addFlashAttribute("err", "Delete course is not implemented yet.");
        return "redirect:/courses";
    }

  
}
