package ua.foxminded.university.web.course;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ua.foxminded.university.service.dto.request.course.CourseCreateDto;
import ua.foxminded.university.service.exception.course.CourseCreateException;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@ControllerAdvice(assignableTypes = CourseCreateController.class)
@RequiredArgsConstructor
public class CourseCreateExceptionHandler {
	
	private final ExceptionMessageReader messageReader;

	@ExceptionHandler(CourseCreateException.class)
    public String handleCourseCreate(CourseCreateException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("form", ex.getForm());
        ra.addFlashAttribute("err", messageReader.safeMessage(ex, "Invalid request."));
        return "redirect:/courses/create";
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleBadRequest(IllegalArgumentException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("form", emptyForm());
        ra.addFlashAttribute("err", messageReader.safeMessage(ex, "Invalid request."));
        return "redirect:/courses/create";
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleValidation(ConstraintViolationException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("form", emptyForm());
        ra.addFlashAttribute("err", messageReader.formatViolations(ex));
        return "redirect:/courses/create";
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleNotFound(EntityNotFoundException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("form", emptyForm());
        ra.addFlashAttribute("err", messageReader.safeMessage(ex, "Teacher not found."));
        return "redirect:/courses/create";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDbConflict(DataIntegrityViolationException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("form", emptyForm());
        ra.addFlashAttribute("err", "Course code or name already exists.");
        return "redirect:/courses/create";
    }

    @ExceptionHandler(Exception.class)
    public String handleOther(Exception ex, RedirectAttributes ra) {
        ra.addFlashAttribute("form", emptyForm());
        ra.addFlashAttribute("err", "Something went wrong.");
        return "redirect:/courses/create";
    }
    
    private CourseCreateDto emptyForm() {
        return new CourseCreateDto(null, null, null, null);
    }
}