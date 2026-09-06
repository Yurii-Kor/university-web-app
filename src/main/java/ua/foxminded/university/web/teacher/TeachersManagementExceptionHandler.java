package ua.foxminded.university.web.teacher;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@ControllerAdvice(assignableTypes = TeachersManagementController.class)
@RequiredArgsConstructor
public class TeachersManagementExceptionHandler {

    private final ExceptionMessageReader messageReader;

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleNotFound(
            EntityNotFoundException ex,
            RedirectAttributes ra) {

        ra.addFlashAttribute(
                "err",
                messageReader.safeMessage(
                        ex,
                        "Teacher not found."));

        return "redirect:/teachers";
    }
}