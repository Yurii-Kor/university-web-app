package ua.foxminded.university.web.profile;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@ControllerAdvice(assignableTypes = ProfileController.class)
@RequiredArgsConstructor
public class ProfileExceptionHandler {
	
	private final ExceptionMessageReader messageReader;

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArg(IllegalArgumentException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("err", messageReader.safeMessage(ex, "Invalid input."));
        return "redirect:/profile";
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleValidation(ConstraintViolationException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("err", messageReader.formatViolations(ex));
        return "redirect:/profile";
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleNotFound(EntityNotFoundException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("err", "User not found. Please log in again.");
        return "redirect:/login";
    }

    @ExceptionHandler(Exception.class)
    public String handleOther(Exception ex, RedirectAttributes ra) {
        ra.addFlashAttribute("err", "Something went wrong.");
        return "redirect:/profile";
    }
}

