package ua.foxminded.university.web.lesson;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.lesson.dto.LessonCreateDto;
import ua.foxminded.university.service.lesson.exception.LessonCreateException;
import ua.foxminded.university.service.lesson.exception.RequestedSlot;
import ua.foxminded.university.service.lesson.exception.ScheduleConflictException;
import ua.foxminded.university.web.util.PrincipalHandler;

@ControllerAdvice(assignableTypes = LessonCreateController.class)
@RequiredArgsConstructor
public class LessonCreateExceptionHandler {

	private static final String FORM_ATTRIBUTE =
			"form";

	private static final String CREATE_VIEW =
			"lessons/create";

	private static final String CREATE_ERROR_CODE =
			"lesson.create";

	private static final String CONFLICT_ERROR_CODE =
			"lesson.schedule.conflict";

	private final ObjectProvider<LessonCreatePageDataLoader> pageDataLoaderProvider;
	private final ObjectProvider<PrincipalHandler> principalHandlerProvider;
	private final ObjectProvider<Clock> clockProvider;

	@ExceptionHandler(LessonCreateException.class)
	public String handleLessonCreate(
	        LessonCreateException ex,
	        @AuthenticationPrincipal UserDetails principal,
	        Model model) {

	    var form = Optional.ofNullable(
	            ex.getForm())
	            .orElseGet(this::emptyForm);

	    var bindingResult =
	            new BeanPropertyBindingResult(
	                    form,
	                    FORM_ATTRIBUTE);

	    var globalMessages =
	            new ArrayList<String>();

	    addViolationErrors(
	            ex,
	            bindingResult,
	            globalMessages);

	    model.addAllAttributes(
	            bindingResult.getModel());

	    addGlobalMessages(
	            model,
	            globalMessages);

	    addPageData(
	            model,
	            principal,
	            form);

	    return CREATE_VIEW;
	}
	
	private void addViolationErrors(
	        LessonCreateException ex,
	        BeanPropertyBindingResult bindingResult,
	        List<String> globalMessages) {

	    ex.getViolations()
	            .forEach(violation ->
	                    Optional.ofNullable(
	                            violation.field())
	                            .filter(this::isFormField)
	                            .ifPresentOrElse(
	                                    field ->
	                                            addFieldError(
	                                                    bindingResult,
	                                                    field,
	                                                    CREATE_ERROR_CODE,
	                                                    violation.message()),
	                                    () ->
	                                            globalMessages.add(
	                                                    violation.message())));
	}

	@ExceptionHandler(ScheduleConflictException.class)
	public String handleScheduleConflict(
	        ScheduleConflictException ex,
	        @AuthenticationPrincipal UserDetails principal,
	        Model model) {

	    var form = toCreateForm(
	            ex.getRequested());

	    var bindingResult =
	            new BeanPropertyBindingResult(
	                    form,
	                    FORM_ATTRIBUTE);

	    addGroupConflictError(
	            bindingResult,
	            ex);

	    addTeacherConflictError(
	            bindingResult,
	            ex);

	    addRoomConflictError(
	            bindingResult,
	            ex);

	    model.addAllAttributes(
	            bindingResult.getModel());

	    model.addAttribute(
	            "schedule_conflict",
	            ex);

	    addPageData(
	            model,
	            principal,
	            form);

	    return CREATE_VIEW;
	}
	
	private void addGroupConflictError(
	        BeanPropertyBindingResult bindingResult,
	        ScheduleConflictException ex) {

	    Optional.ofNullable(
	            ex.getGroupConflicts())
	            .filter(conflicts ->
	                    !conflicts.isEmpty())
	            .ifPresent(conflicts ->
	                    addFieldError(
	                            bindingResult,
	                            "groupId",
	                            CONFLICT_ERROR_CODE,
	                            "Selected study group already has a lesson during this time."));
	}

	private void addTeacherConflictError(
	        BeanPropertyBindingResult bindingResult,
	        ScheduleConflictException ex) {

	    Optional.ofNullable(
	            ex.getTeacherConflicts())
	            .filter(conflicts ->
	                    !conflicts.isEmpty())
	            .ifPresent(conflicts ->
	                    addFieldError(
	                            bindingResult,
	                            "startTime",
	                            CONFLICT_ERROR_CODE,
	                            "You already have another lesson during this time."));
	}

	private void addRoomConflictError(
	        BeanPropertyBindingResult bindingResult,
	        ScheduleConflictException ex) {

	    Optional.ofNullable(
	            ex.getRoomConflicts())
	            .filter(conflicts ->
	                    !conflicts.isEmpty())
	            .ifPresent(conflicts ->
	                    addFieldError(
	                            bindingResult,
	                            "room",
	                            CONFLICT_ERROR_CODE,
	                            "Selected room is already occupied during this time."));
	}

	@ExceptionHandler(BindException.class)
	public String handleValidation(
			BindException ex,
			@AuthenticationPrincipal UserDetails principal,
			Model model) {

		var form = Optional.ofNullable(
				ex.getBindingResult()
						.getTarget())
				.filter(
						LessonCreateDto.class::isInstance)
				.map(
						LessonCreateDto.class::cast)
				.orElseGet(this::emptyForm);

		model.addAllAttributes(
				ex.getBindingResult()
						.getModel());

		addPageData(
				model,
				principal,
				form);

		return CREATE_VIEW;
	}

	private void addPageData(
			Model model,
			UserDetails principal,
			LessonCreateDto form) {

		var teacherId =
				principalHandlerProvider.getObject()
						.parseUserId(principal);

		pageDataLoaderProvider.getObject()
				.addPageData(
						model,
						teacherId,
						form);
	}

	private LessonCreateDto toCreateForm(
			RequestedSlot requested) {

		var clock =
				clockProvider.getObject();

		var startTime =
				requested.startTime()
						.atZoneSameInstant(
								clock.getZone());

		var endTime =
				requested.endTime()
						.atZoneSameInstant(
								clock.getZone());

		return new LessonCreateDto(
				requested.courseId(),
				requested.groupId(),
				startTime.toLocalDate(),
				startTime.toLocalTime(),
				endTime.toLocalTime(),
				requested.room(),
				requested.lessonType(),
				requested.description());
	}

	private void addFieldError(
			BeanPropertyBindingResult bindingResult,
			String field,
			String errorCode,
			String message) {

		bindingResult.addError(
				new FieldError(
						FORM_ATTRIBUTE,
						field,
						null,
						false,
						new String[] { errorCode },
						null,
						message));
	}

	private void addGlobalMessages(
			Model model,
			List<String> messages) {

		if (!messages.isEmpty()) {
			model.addAttribute(
					"err",
					String.join(
							" ",
							messages));
		}
	}

	private boolean isFormField(
			String field) {

		return Arrays.stream(
				LessonCreateDto.class
						.getRecordComponents())
				.anyMatch(component ->
						component.getName()
								.equals(field));
	}

	private LessonCreateDto emptyForm() {
		return new LessonCreateDto(
				null,
				null,
				LocalDate.now(
						clockProvider.getObject()),
				null,
				null,
				null,
				null,
				null);
	}
}