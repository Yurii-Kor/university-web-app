package ua.foxminded.university.web.lesson;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@ControllerAdvice(assignableTypes = LessonDeleteController.class)
@RequiredArgsConstructor
public class LessonDeleteExceptionHandler {

    private final ExceptionMessageReader messageReader;

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute(
                "err",
                messageReader.safeMessage(
                        ex,
                        "Lesson not found."));

        return redirectToDay(
                request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleCannotDelete(
            IllegalStateException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute(
                "err",
                messageReader.safeMessage(
                        ex,
                        "Lesson cannot be deleted."));

        return redirectToDay(
                request);
    }

    private String redirectToDay(
            HttpServletRequest request) {

        var date =
                request.getParameter(
                        "date");

        if (date == null
                || date.isBlank()) {

            return "redirect:/schedule/day";
        }

        try {

            var parsedDate =
                    LocalDate.parse(
                            date);

            return "redirect:/schedule/day?date="
                    + parsedDate;

        } catch (DateTimeParseException ex) {

            return "redirect:/schedule/day";
        }
    }
}