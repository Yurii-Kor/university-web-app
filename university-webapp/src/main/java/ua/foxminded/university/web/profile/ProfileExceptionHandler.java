package ua.foxminded.university.web.profile;

import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

@ControllerAdvice(assignableTypes = ProfileController.class)
public class ProfileExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArg(IllegalArgumentException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("err", safeMessage(ex, "Invalid input."));
        return "redirect:/profile";
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleValidation(ConstraintViolationException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("err", formatViolations(ex));
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

    private String formatViolations(ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
    }

    private String safeMessage(Exception ex, String fallback) {
        return Optional.ofNullable(ex.getMessage()).filter(m -> !m.isBlank()).orElse(fallback);
    }
}

