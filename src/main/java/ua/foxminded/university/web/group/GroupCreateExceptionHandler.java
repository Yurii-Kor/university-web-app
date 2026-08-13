package ua.foxminded.university.web.group;

import jakarta.validation.ConstraintViolationException;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ua.foxminded.university.service.studygroup.dto.StudyGroupCreateDto;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@ControllerAdvice(assignableTypes = GroupCreateController.class)
@RequiredArgsConstructor
public class GroupCreateExceptionHandler {

    private final ExceptionMessageReader messageReader;

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleBadRequest(
            IllegalArgumentException ex,
            RedirectAttributes ra) {

        ra.addFlashAttribute(
                "form",
                emptyForm());

        ra.addFlashAttribute(
                "err",
                messageReader.safeMessage(
                        ex,
                        "Invalid request."));

        return "redirect:/groups/create";
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleValidation(
            ConstraintViolationException ex,
            RedirectAttributes ra) {

        ra.addFlashAttribute(
                "form",
                emptyForm());

        ra.addFlashAttribute(
                "err",
                messageReader.formatViolations(ex));

        return "redirect:/groups/create";
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDbConflict(
            DataIntegrityViolationException ex,
            RedirectAttributes ra) {

        ra.addFlashAttribute(
                "form",
                emptyForm());

        ra.addFlashAttribute(
                "err",
                "Study group name already exists.");

        return "redirect:/groups/create";
    }

    private StudyGroupCreateDto emptyForm() {
        return new StudyGroupCreateDto(null);
    }
}