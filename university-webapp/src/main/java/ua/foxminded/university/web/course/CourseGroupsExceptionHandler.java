package ua.foxminded.university.web.course;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.exception.course.CourseGroupsOpException;
import ua.foxminded.university.web.util.ExceptionMessageReader;

import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(assignableTypes = CourseGroupsController.class)
@RequiredArgsConstructor
public class CourseGroupsExceptionHandler {
	
	private final ExceptionMessageReader messageReader;

    @ExceptionHandler(CourseGroupsOpException.class)
    public String handleOp(CourseGroupsOpException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("err", messageReader.safeMessage(ex, "Operation failed."));
        return "redirect:/courses/" + ex.getCourseId() + "/groups";
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleNotFound(EntityNotFoundException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("err", messageReader.safeMessage(ex, "Course not found."));
        return "redirect:/courses";
    }

    @ExceptionHandler({ IllegalArgumentException.class, DataAccessException.class })
    public String handleOther(RuntimeException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("err", messageReader.safeMessage(ex, "Operation failed."));
        return "redirect:/courses";
    }
}
