package ua.foxminded.university.web.account;

import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.dto.request.appuser.StudentToTeacherRoleChangeDto;
import ua.foxminded.university.service.dto.request.appuser.TeacherToStudentRoleChangeDto;
import ua.foxminded.university.service.exception.appuser.StudentToTeacherRoleChangeException;
import ua.foxminded.university.service.exception.appuser.TeacherToStudentRoleChangeException;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@ControllerAdvice(assignableTypes = AccountManagementController.class)
@RequiredArgsConstructor
public class AccountManagementExceptionHandler {

    private final ExceptionMessageReader messageReader;

    @ExceptionHandler(StudentToTeacherRoleChangeException.class)
    public String handleStudentToTeacher(StudentToTeacherRoleChangeException ex,
                                         RedirectAttributes ra) {

        Optional.ofNullable(ex.getForm())
                .map(StudentToTeacherRoleChangeDto::userId)
                .ifPresent(id -> ra.addFlashAttribute("accountId", id));

        ra.addFlashAttribute("err", messageReader.safeMessage(ex, "Unable to change student role."));
        return "redirect:/accounts?view=students";
    }

    @ExceptionHandler(TeacherToStudentRoleChangeException.class)
    public String handleTeacherToStudent(TeacherToStudentRoleChangeException ex,
                                         RedirectAttributes ra) {

        Optional.ofNullable(ex.getForm())
                .map(TeacherToStudentRoleChangeDto::userId)
                .ifPresent(id -> ra.addFlashAttribute("accountId", id));

        ra.addFlashAttribute("err", messageReader.safeMessage(ex, "Unable to change teacher role."));
        return "redirect:/accounts?view=teachers";
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleValidation(ConstraintViolationException ex,
                                   RedirectAttributes ra) {

        ra.addFlashAttribute("err", messageReader.formatViolations(ex));
        return "redirect:/accounts";
    }

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public String handleBadRequest(RuntimeException ex,
                                   RedirectAttributes ra) {

        ra.addFlashAttribute("err", messageReader.safeMessage(ex, "Invalid request."));
        return "redirect:/accounts";
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleNotFound(EntityNotFoundException ex,
                                 RedirectAttributes ra) {

        ra.addFlashAttribute("err", messageReader.safeMessage(ex, "Account not found."));
        return "redirect:/accounts";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex,
                                     RedirectAttributes ra) {

        ra.addFlashAttribute("err", "Access denied.");
        return "redirect:/accounts";
    }

    @ExceptionHandler(Exception.class)
    public String handleOther(Exception ex,
                              RedirectAttributes ra) {

        ra.addFlashAttribute("err", "Something went wrong.");
        return "redirect:/accounts";
    }
}