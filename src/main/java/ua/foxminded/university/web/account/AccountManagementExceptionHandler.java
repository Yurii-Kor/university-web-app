package ua.foxminded.university.web.account;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.service.rolechange.exception.RoleChangeException;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@ControllerAdvice(assignableTypes = AccountManagementController.class)
@RequiredArgsConstructor
public class AccountManagementExceptionHandler {

    private final ExceptionMessageReader messageReader;

    @ExceptionHandler(RoleChangeException.class)
    public String handleRoleChange(RoleChangeException ex,
                                   RedirectAttributes ra) {

        ra.addFlashAttribute("err", messageReader.safeMessage(ex, "Unable to change account role."));
        ra.addFlashAttribute("accountId", ex.accountId());

        return redirectToRoleView(ex.currentRole());
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

    private String redirectToRoleView(UserRole role) {
        return switch (role) {
            case STUDENT -> "redirect:/accounts?view=students&page=0";
            case TEACHER -> "redirect:/accounts?view=teachers&page=0";
            case ADMIN -> "redirect:/accounts?page=0";
        };
    }
}