package ua.foxminded.university.web.profile;

import java.util.Objects;

import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@ControllerAdvice(assignableTypes = PasswordChangeController.class)
@RequiredArgsConstructor
public class PasswordChangeExceptionHandler {

    private final ExceptionMessageReader messageReader;

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArg(
            IllegalArgumentException ex,
            RedirectAttributes ra) {

        ra.addFlashAttribute(
                "err",
                messageReader.safeMessage(
                        ex,
                        "Invalid input."));

        return "redirect:/profile/password";
    }

    @ExceptionHandler(BindException.class)
    public String handleValidation(
            BindException ex,
            RedirectAttributes ra) {

        var message =
                ex.getBindingResult()
                        .getAllErrors()
                        .stream()
                        .map(error ->
                                error.getDefaultMessage())
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse("Invalid input.");

        ra.addFlashAttribute(
                "err",
                message);

        return "redirect:/profile/password";
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleNotFound(
            EntityNotFoundException ex,
            RedirectAttributes ra) {

        ra.addFlashAttribute(
                "err",
                "User not found. Please log in again.");

        return "redirect:/login";
    }

    @ExceptionHandler(Exception.class)
    public String handleOther(
            Exception ex,
            RedirectAttributes ra) {

        ra.addFlashAttribute(
                "err",
                "Something went wrong.");

        return "redirect:/profile/password";
    }
}