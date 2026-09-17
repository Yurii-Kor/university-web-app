package ua.foxminded.university.web.student;

import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.student.dto.StudentCreateDto;
import ua.foxminded.university.service.student.exception.StudentCreateException;
import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@ControllerAdvice(assignableTypes = StudentCreateController.class)
@RequiredArgsConstructor
public class StudentCreateExceptionHandler {

	private static final String FORM_ATTRIBUTE = "form";
	private static final String CREATE_VIEW = "students/create";
	private static final String CREATE_REDIRECT = "redirect:/students/create";
	private static final String CREATE_ERROR_CODE = "student.create";

	private final ObjectProvider<StudyGroupService> groupServiceProvider;
	private final ExceptionMessageReader messageReader;

	@ExceptionHandler(StudentCreateException.class)
	public String handleStudentCreate(
			StudentCreateException ex,
			Model model) {

		var message = messageReader.safeMessage(
				ex,
				"Student cannot be created.");

		var form = Optional.ofNullable(ex.getForm())
				.orElseGet(this::emptyForm);

		if (isFormField(ex.getField())) {
			addFieldError(
					model,
					form,
					ex.getField(),
					message);
		} else {
			model.addAttribute(
					FORM_ATTRIBUTE,
					form);

			model.addAttribute(
					"err",
					message);
		}

		addPageData(model);

		return CREATE_VIEW;
	}

	@ExceptionHandler(BindException.class)
	public String handleValidation(
			BindException ex,
			Model model) {

		model.addAllAttributes(
				ex.getBindingResult()
						.getModel());

		addPageData(model);

		return CREATE_VIEW;
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public String handleBadRequest(
			IllegalArgumentException ex,
			RedirectAttributes ra) {

		ra.addFlashAttribute(
				FORM_ATTRIBUTE,
				emptyForm());

		ra.addFlashAttribute(
				"err",
				messageReader.safeMessage(
						ex,
						"Invalid request."));

		return CREATE_REDIRECT;
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public String handleValidation(
			ConstraintViolationException ex,
			RedirectAttributes ra) {

		ra.addFlashAttribute(
				FORM_ATTRIBUTE,
				emptyForm());

		ra.addFlashAttribute(
				"err",
				messageReader.formatViolations(ex));

		return CREATE_REDIRECT;
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public String handleDbConflict(
			DataIntegrityViolationException ex,
			RedirectAttributes ra) {

		ra.addFlashAttribute(
				FORM_ATTRIBUTE,
				emptyForm());

		ra.addFlashAttribute(
				"err",
				"Student account data conflicts with an existing record.");

		return CREATE_REDIRECT;
	}

	private void addFieldError(
			Model model,
			StudentCreateDto form,
			String field,
			String message) {

		var bindingResult = new BeanPropertyBindingResult(
				form,
				FORM_ATTRIBUTE);

		bindingResult.addError(
				new FieldError(
						FORM_ATTRIBUTE,
						field,
						null,
						false,
						new String[] {
								CREATE_ERROR_CODE
						},
						null,
						message));

		model.addAllAttributes(
				bindingResult.getModel());
	}

	private boolean isFormField(String field) {

		return Optional.ofNullable(field)
				.map(this::isRecordComponent)
				.orElse(false);
	}

	private boolean isRecordComponent(String field) {

		return Arrays.stream(
				StudentCreateDto.class
						.getRecordComponents())
				.anyMatch(component ->
						component.getName()
								.equals(field));
	}

	private void addPageData(Model model) {

		model.addAttribute(
				"all_groups",
				groupServiceProvider.getObject()
						.listGroupOptions());
	}

	private StudentCreateDto emptyForm() {
		return new StudentCreateDto();
	}
}