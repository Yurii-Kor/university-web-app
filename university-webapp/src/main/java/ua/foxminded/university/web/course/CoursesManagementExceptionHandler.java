package ua.foxminded.university.web.course;

import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(assignableTypes = CoursesManagementController.class)
public class CoursesManagementExceptionHandler {

    @ExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    public String handleBadRequest(RuntimeException ex,
                                   HttpServletRequest req,
                                   RedirectAttributes ra) {

        addCourseContext(req, ra);
        ra.addFlashAttribute("err", safeMessage(ex, "Invalid input."));
        return "redirect:/courses";
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleValidation(ConstraintViolationException ex,
                                   HttpServletRequest req,
                                   RedirectAttributes ra) {

        addCourseContext(req, ra);
        ra.addFlashAttribute("err", formatViolations(ex));
        return "redirect:/courses";
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleNotFound(EntityNotFoundException ex,
                                 HttpServletRequest req,
                                 RedirectAttributes ra) {

        addCourseContext(req, ra);
        ra.addFlashAttribute("err", safeMessage(ex, "Course not found."));
        return "redirect:/courses";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex,
                                     HttpServletRequest req,
                                     RedirectAttributes ra) {

        addCourseContext(req, ra);
        ra.addFlashAttribute("err", "Access denied.");
        return "redirect:/courses";
    }

    @ExceptionHandler(Exception.class)
    public String handleOther(Exception ex,
                              HttpServletRequest req,
                              RedirectAttributes ra) {

        addCourseContext(req, ra);
        ra.addFlashAttribute("err", "Something went wrong.");
        return "redirect:/courses";
    }

    private void addCourseContext(HttpServletRequest req, RedirectAttributes ra) {
        var courseId = resolveCourseId(req);
        var op = resolveOp(req);

        courseId.ifPresent(id -> ra.addFlashAttribute("courseId", id));
        ra.addFlashAttribute("courseOp", op);
    }

    private Optional<Long> resolveCourseId(HttpServletRequest req) {
        var idParam = req.getParameter("id");
        if (idParam != null && !idParam.isBlank()) {
            try {
                return Optional.of(Long.parseLong(idParam.trim()));
            } catch (NumberFormatException ignored) {}
        }

        var uri = Optional.ofNullable(req.getRequestURI()).orElse("");

        var marker = "/courses/";
        var idx = uri.indexOf(marker);
        if (idx >= 0) {
            var rest = uri.substring(idx + marker.length());
            var slash = rest.indexOf('/');
            if (slash > 0) {
                var first = rest.substring(0, slash);
                try {
                    return Optional.of(Long.parseLong(first));
                } catch (NumberFormatException ignored) {}
            }
        }

        return Optional.empty();
    }

    private String resolveOp(HttpServletRequest req) {
        var uri = Optional.ofNullable(req.getRequestURI()).orElse("");
        if (uri.endsWith("/description/update")) return "description";
        if (uri.endsWith("/self/update")) return "self";
        if (uri.endsWith("/delete")) return "delete";
        return "unknown";
    }

    private String formatViolations(ConstraintViolationException ex) {
        var msg = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
        return msg.isBlank() ? "Invalid input." : msg;
    }

    private String safeMessage(Exception ex, String fallback) {
        return Optional.ofNullable(ex.getMessage())
                .map(String::trim)
                .filter(m -> !m.isBlank())
                .orElse(fallback);
    }
}
