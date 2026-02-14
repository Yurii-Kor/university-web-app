package ua.foxminded.university.web.course;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.repository.dto.TeacherOptionView;
import ua.foxminded.university.service.CourseService;
import ua.foxminded.university.service.TeacherService;
import ua.foxminded.university.web.course.dto.CourseDescriptionUpdateForm;
import ua.foxminded.university.web.course.dto.CourseFormMapper;
import ua.foxminded.university.web.course.dto.CourseSelfUpdateForm;
import ua.foxminded.university.web.util.PrincipalHandler;

@Controller
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CoursesManagementController {

    private final CourseService courseService;
    private final TeacherService teacherService;
    private final PrincipalHandler principalHandler;
    private final CourseFormMapper mapper;

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

    @PostMapping("/description/update")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public String updateDescription(@ModelAttribute CourseDescriptionUpdateForm patch,
                                    RedirectAttributes ra) {

        courseService.updateDescription(mapper.toDescriptionUpdateDto(patch));

        ra.addFlashAttribute("ok", "Description updated.");
        ra.addFlashAttribute("courseId", patch.id());
        ra.addFlashAttribute("courseOp", "description");

        return "redirect:/courses";
    }

    @PostMapping("/self/update")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateSelf(@ModelAttribute CourseSelfUpdateForm patch,
                             RedirectAttributes ra) {

        courseService.updateSelf(mapper.toSelfUpdateDto(patch));

        ra.addFlashAttribute("ok", "Course updated.");
        ra.addFlashAttribute("courseId", patch.id());
        ra.addFlashAttribute("courseOp", "self");

        return "redirect:/courses";
    }
    
    @ModelAttribute("teachers")
    public List<TeacherOptionView> teachers() {
        return teacherService.listTeacherOptions();
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
