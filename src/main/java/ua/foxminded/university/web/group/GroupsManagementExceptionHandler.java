package ua.foxminded.university.web.group;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ua.foxminded.university.service.studygroup.exception.StudyGroupDeleteException;
import ua.foxminded.university.service.studygroup.exception.StudyGroupRenameException;
import ua.foxminded.university.service.util.validation.ContextConstraintViolationException;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@ControllerAdvice(assignableTypes = GroupsManagementController.class)
@RequiredArgsConstructor
public class GroupsManagementExceptionHandler {

    private final ExceptionMessageReader messageReader;

    @ExceptionHandler(StudyGroupRenameException.class)
    public String handleRename(
            StudyGroupRenameException ex,
            RedirectAttributes ra) {

        ra.addFlashAttribute(
                "groupId",
                ex.getGroupId());

        ra.addFlashAttribute(
                "requestedName",
                ex.getRequestedName());

        ra.addFlashAttribute(
                "err",
                messageReader.safeMessage(
                        ex,
                        "Group cannot be renamed."));

        return "redirect:/groups";
    }

    @ExceptionHandler(StudyGroupDeleteException.class)
    public String handleDelete(
            StudyGroupDeleteException ex,
            RedirectAttributes ra) {

        ra.addFlashAttribute(
                "groupId",
                ex.getGroupId());

        ra.addFlashAttribute(
                "err",
                messageReader.safeMessage(
                        ex,
                        "Group cannot be deleted."));

        return "redirect:/groups";
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleValidation(
            ConstraintViolationException ex,
            RedirectAttributes ra) {

        if (ex instanceof ContextConstraintViolationException ctx) {
            ctx.entityId()
                    .ifPresent(id ->
                            ra.addFlashAttribute(
                                    "groupId",
                                    id));
        }

        ra.addFlashAttribute(
                "err",
                messageReader.formatViolations(ex));

        return "redirect:/groups";
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

        return "redirect:/groups";
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleNotFound(
            EntityNotFoundException ex,
            RedirectAttributes ra) {

        ra.addFlashAttribute(
                "err",
                messageReader.safeMessage(
                        ex,
                        "Study group not found."));

        return "redirect:/groups";
    }
}