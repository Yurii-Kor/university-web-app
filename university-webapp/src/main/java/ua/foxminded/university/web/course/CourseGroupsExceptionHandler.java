package ua.foxminded.university.web.course;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(assignableTypes = CourseGroupsController.class)
public class CourseGroupsExceptionHandler {

    @ExceptionHandler({ EntityNotFoundException.class, IllegalArgumentException.class, DataAccessException.class })
    public String handleKnown(RuntimeException ex,
                              HttpServletRequest request,
                              RedirectAttributes ra) {

        String msg = (ex.getMessage() == null || ex.getMessage().isBlank())
                ? "Operation failed."
                : ex.getMessage();

        ra.addFlashAttribute("err", msg);
        return "redirect:" + groupsPagePath(request);
    }

    private String groupsPagePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        String path = (ctx != null && !ctx.isBlank() && uri.startsWith(ctx)) ? uri.substring(ctx.length()) : uri;

        int idx = path.indexOf("/groups");
        if (idx == -1) return "/courses";

        return path.substring(0, idx + "/groups".length());
    }
}
