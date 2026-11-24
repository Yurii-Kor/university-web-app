package ua.foxminded.university.web.lesson;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.service.lesson.dto.LessonUpdateDto;
import ua.foxminded.university.service.lesson.exception.LessonUpdateException;
import ua.foxminded.university.service.lesson.dto.RequestedSlot;
import ua.foxminded.university.service.lesson.exception.ScheduleConflictException;
import ua.foxminded.university.web.util.PrincipalHandler;

@ControllerAdvice(assignableTypes = LessonUpdateController.class)
@RequiredArgsConstructor
public class LessonUpdateExceptionHandler {

    private static final String FORM_ATTRIBUTE =
            "form";

    private static final String EDIT_VIEW =
            "lessons/edit";

    private static final String UPDATE_ERROR_CODE =
            "lesson.update";

    private static final String CONFLICT_ERROR_CODE =
            "lesson.schedule.conflict";

    private final ObjectProvider<LessonUpdatePageDataLoader> pageDataLoaderProvider;
    private final ObjectProvider<PrincipalHandler> principalHandlerProvider;
    private final ObjectProvider<Clock> clockProvider;

    @ExceptionHandler(LessonUpdateException.class)
    public String handleLessonUpdate(
            LessonUpdateException ex,
            @AuthenticationPrincipal UserDetails principal,
            Model model) {

        var form =
                Optional.ofNullable(
                        ex.getForm())
                        .orElseGet(
                                this::emptyForm);

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
                ex.getLessonId(),
                form);

        return EDIT_VIEW;
    }

    @ExceptionHandler(ScheduleConflictException.class)
    public String handleScheduleConflict(
            ScheduleConflictException ex,
            @AuthenticationPrincipal UserDetails principal,
            Model model) {

        var lessonId =
                Optional.ofNullable(
                        ex.getLessonId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Lesson id is required for update schedule conflict."));

        var form =
                toUpdateForm(
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
                lessonId,
                form);

        return EDIT_VIEW;
    }

    private void addViolationErrors(
            LessonUpdateException ex,
            BeanPropertyBindingResult bindingResult,
            List<String> globalMessages) {

        ex.getViolations()
                .forEach(violation ->
                        Optional.ofNullable(
                                violation.field())
                                .filter(
                                        this::isFormField)
                                .ifPresentOrElse(
                                        field ->
                                                addFieldError(
                                                        bindingResult,
                                                        field,
                                                        UPDATE_ERROR_CODE,
                                                        violation.message()),
                                        () ->
                                                globalMessages.add(
                                                        violation.message())));
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
                                "startTime",
                                CONFLICT_ERROR_CODE,
                                "The study group already has another lesson during this time."));
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

    private LessonUpdateDto toUpdateForm(
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

        return new LessonUpdateDto(
                startTime.toLocalDate(),
                startTime.toLocalTime(),
                endTime.toLocalTime(),
                requested.room(),
                requested.lessonType(),
                requested.description());
    }

    private void addPageData(
            Model model,
            UserDetails principal,
            long lessonId,
            LessonUpdateDto form) {

        var teacherId =
                principalHandlerProvider.getObject()
                        .parseUserId(
                                principal);

        pageDataLoaderProvider.getObject()
                .addPageData(
                        model,
                        teacherId,
                        lessonId,
                        form);
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
                LessonUpdateDto.class
                        .getRecordComponents())
                .anyMatch(component ->
                        component.getName()
                                .equals(field));
    }

    private LessonUpdateDto emptyForm() {
        return new LessonUpdateDto();
    }
}