package ua.foxminded.university.web.student;

import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
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

    private final ObjectProvider<StudyGroupService> groupServiceProvider;
    private final ExceptionMessageReader messageReader;

    @ExceptionHandler(StudentCreateException.class)
    public String handleStudentCreate(
            StudentCreateException ex,
            Model model) {

        var message = messageReader.safeMessage(
                ex,
                "Student cannot be created.");

        var form = clearRejectedField(
                ex.getForm(),
                ex.getField());

        var bindingResult = new BeanPropertyBindingResult(
                form,
                FORM_ATTRIBUTE);

        if (isFormField(ex.getField())) {
            bindingResult.rejectValue(
                    ex.getField(),
                    "student.create",
                    message);

            model.addAllAttributes(
                    bindingResult.getModel());
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

    private void addPageData(Model model) {

        model.addAttribute(
                "all_groups",
                groupServiceProvider.getObject()
                        .listGroupOptions());
    }

    private StudentCreateDto clearRejectedField(
            StudentCreateDto form,
            String field) {

        return Optional.ofNullable(form)
                .map(value -> clearField(
                        value,
                        field))
                .orElseGet(this::emptyForm);
    }

    private StudentCreateDto clearField(
            StudentCreateDto form,
            String field) {

        return Optional.ofNullable(field)
                .map(value -> switch (value) {
                    case "email" -> withoutEmail(form);
                    case "password" -> withoutPassword(form);
                    case "firstName" -> withoutFirstName(form);
                    case "lastName" -> withoutLastName(form);
                    case "groupId" -> withoutGroup(form);
                    case "enrollmentYear" -> withoutEnrollmentYear(form);
                    default -> form;
                })
                .orElse(form);
    }

    private StudentCreateDto withoutEmail(
            StudentCreateDto form) {

        return new StudentCreateDto(
                null,
                form.password(),
                form.firstName(),
                form.lastName(),
                form.groupId(),
                form.enrollmentYear());
    }

    private StudentCreateDto withoutPassword(
            StudentCreateDto form) {

        return new StudentCreateDto(
                form.email(),
                null,
                form.firstName(),
                form.lastName(),
                form.groupId(),
                form.enrollmentYear());
    }

    private StudentCreateDto withoutFirstName(
            StudentCreateDto form) {

        return new StudentCreateDto(
                form.email(),
                form.password(),
                null,
                form.lastName(),
                form.groupId(),
                form.enrollmentYear());
    }

    private StudentCreateDto withoutLastName(
            StudentCreateDto form) {

        return new StudentCreateDto(
                form.email(),
                form.password(),
                form.firstName(),
                null,
                form.groupId(),
                form.enrollmentYear());
    }

    private StudentCreateDto withoutGroup(
            StudentCreateDto form) {

        return new StudentCreateDto(
                form.email(),
                form.password(),
                form.firstName(),
                form.lastName(),
                null,
                form.enrollmentYear());
    }

    private StudentCreateDto withoutEnrollmentYear(
            StudentCreateDto form) {

        return new StudentCreateDto(
                form.email(),
                form.password(),
                form.firstName(),
                form.lastName(),
                form.groupId(),
                null);
    }

    private boolean isFormField(String field) {

        return Optional.ofNullable(field)
                .map(value -> switch (value) {
                    case "email",
                         "password",
                         "firstName",
                         "lastName",
                         "groupId",
                         "enrollmentYear" -> true;

                    default -> false;
                })
                .orElse(false);
    }

    private StudentCreateDto emptyForm() {

        return new StudentCreateDto(
                null,
                null,
                null,
                null,
                null,
                null);
    }
}