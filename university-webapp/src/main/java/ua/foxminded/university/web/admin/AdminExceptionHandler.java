package ua.foxminded.university.web.admin;

import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

@ControllerAdvice(assignableTypes = AdminManagementController.class)
public class AdminExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleCreateBadRequest(IllegalArgumentException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("err", safeMessage(ex, "Invalid request."));
        return "redirect:/admin/create";
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleCreateValidation(ConstraintViolationException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("err", formatViolations(ex));
        return "redirect:/admin/create";
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleState(IllegalStateException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("err", safeMessage(ex, "Operation is not allowed."));
        return "redirect:/admin";
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleNotFound(EntityNotFoundException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("err", safeMessage(ex, "User not found."));
        return "redirect:/admin";
    }

    @ExceptionHandler(Exception.class)
    public String handleOther(Exception ex, RedirectAttributes ra) {
        ra.addFlashAttribute("err", "Something went wrong.");
        return "redirect:/admin";
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
