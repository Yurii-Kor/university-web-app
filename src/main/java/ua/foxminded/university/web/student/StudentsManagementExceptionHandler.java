package ua.foxminded.university.web.student;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.student.exception.StudentUpdateException;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@ControllerAdvice(assignableTypes = StudentsManagementController.class)
@RequiredArgsConstructor
public class StudentsManagementExceptionHandler {

    private final ExceptionMessageReader messageReader;

    @ExceptionHandler(StudentUpdateException.class)
    public String handleUpdate(
            StudentUpdateException ex,
            RedirectAttributes ra) {

        ra.addFlashAttribute(
                "studentId",
                ex.getStudentId());

        ra.addFlashAttribute(
                "err",
                messageReader.safeMessage(
                        ex,
                        "Student cannot be updated."));

        return "redirect:/students";
    }

    @ExceptionHandler({
        IllegalArgumentException.class,
        IllegalStateException.class
    })
    public String handleBadRequest(
            RuntimeException ex,
            RedirectAttributes ra) {

        ra.addFlashAttribute(
                "err",
                messageReader.safeMessage(
                        ex,
                        "Invalid input."));

        return "redirect:/students";
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleNotFound(
            EntityNotFoundException ex,
            RedirectAttributes ra) {

        ra.addFlashAttribute(
                "err",
                messageReader.safeMessage(
                        ex,
                        "Student not found."));

        return "redirect:/students";
    }
}