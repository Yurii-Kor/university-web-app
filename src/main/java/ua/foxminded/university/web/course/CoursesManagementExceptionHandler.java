package ua.foxminded.university.web.course;

import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ua.foxminded.university.service.dto.request.course.CourseSelfUpdateDto;
import ua.foxminded.university.service.exception.course.CourseSelfUpdateException;
import ua.foxminded.university.service.util.validation.ContextConstraintViolationException;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@ControllerAdvice(assignableTypes = CoursesManagementController.class)
@RequiredArgsConstructor
public class CoursesManagementExceptionHandler {
	
	private final ExceptionMessageReader messageReader;

    @ExceptionHandler(CourseSelfUpdateException.class)
    public String handleSelfUpdate(CourseSelfUpdateException ex,
                                   RedirectAttributes ra) {

        Optional.ofNullable(ex.getForm())
                .map(CourseSelfUpdateDto::id)
                .ifPresent(id -> ra.addFlashAttribute("courseId", id));

        ra.addFlashAttribute("err", messageReader.safeMessage(ex, "Invalid input."));
        return "redirect:/courses";
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public String handleValidation(ConstraintViolationException ex,
                                   RedirectAttributes ra) {

        if (ex instanceof ContextConstraintViolationException ctx) {
            ctx.entityId().ifPresent(id -> ra.addFlashAttribute("courseId", id));
        }

        ra.addFlashAttribute("err", messageReader.formatViolations(ex));
        return "redirect:/courses";
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public String handleBadRequest(RuntimeException ex,
                                   RedirectAttributes ra) {

        ra.addFlashAttribute("err", messageReader.safeMessage(ex, "Invalid input."));
        return "redirect:/courses";
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleNotFound(EntityNotFoundException ex,
                                 RedirectAttributes ra) {

        ra.addFlashAttribute("err", messageReader.safeMessage(ex, "Course not found."));
        return "redirect:/courses";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex,
                                     RedirectAttributes ra) {

        ra.addFlashAttribute("err", "Access denied.");
        return "redirect:/courses";
    }

    @ExceptionHandler(Exception.class)
    public String handleOther(Exception ex,
                              RedirectAttributes ra) {

        ra.addFlashAttribute("err", "Something went wrong.");
        return "redirect:/courses";
    }
}