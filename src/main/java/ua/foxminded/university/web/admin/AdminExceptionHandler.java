package ua.foxminded.university.web.admin;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.appuser.exception.AdminCreateException;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@ControllerAdvice(assignableTypes = AdminManagementController.class)
@RequiredArgsConstructor
public class AdminExceptionHandler {

	private final ExceptionMessageReader messageReader;

	@ExceptionHandler(AdminCreateException.class)
	public String handleAdminCreate(AdminCreateException ex, RedirectAttributes ra) {
		ra.addFlashAttribute("err", messageReader.safeMessage(ex, "Unable to create admin."));
		return "redirect:/admin/create";
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public String handleValidation(ConstraintViolationException ex, RedirectAttributes ra) {
		ra.addFlashAttribute("err", messageReader.formatViolations(ex));
		return "redirect:/admin/create";
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public String handleBadRequest(IllegalArgumentException ex, RedirectAttributes ra) {
		ra.addFlashAttribute("err", messageReader.safeMessage(ex, "Invalid request."));
		return "redirect:/admin";
	}

	@ExceptionHandler(IllegalStateException.class)
	public String handleState(IllegalStateException ex, RedirectAttributes ra) {
		ra.addFlashAttribute("err", messageReader.safeMessage(ex, "Operation is not allowed."));
		return "redirect:/admin";
	}

	@ExceptionHandler(EntityNotFoundException.class)
	public String handleNotFound(EntityNotFoundException ex, RedirectAttributes ra) {
		ra.addFlashAttribute("err", messageReader.safeMessage(ex, "User not found."));
		return "redirect:/admin";
	}

	@ExceptionHandler(Exception.class)
	public String handleOther(Exception ex, RedirectAttributes ra) {
		ra.addFlashAttribute("err", "Something went wrong.");
		return "redirect:/admin";
	}
}