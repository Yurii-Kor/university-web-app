package ua.foxminded.university.web.lesson;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static ua.foxminded.university.web.lesson.ScheduleControllerWebMvcTestConstants.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.ui.Model;

import ua.foxminded.university.TestClockConfig;
import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.model.persistence.lesson.projection.OverlapHit;
import ua.foxminded.university.service.lesson.LessonPolicyViolation;
import ua.foxminded.university.service.lesson.exception.LessonUpdateException;
import ua.foxminded.university.service.lesson.exception.RequestedSlot;
import ua.foxminded.university.service.lesson.exception.ScheduleConflictException;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@WebMvcTest(controllers = LessonUpdateController.class)
@Import({
        LessonUpdateExceptionHandler.class,
        MethodSecurityTestConfig.class,
        ExceptionMessageReader.class,
        TestClockConfig.class
})
class LessonUpdateExceptionHandlerWebMvcTest
        extends AbstractLessonUpdateControllerWebMvcTest {

    @Test
    @DisplayName("POST /lessons/{id}/edit policy field violation -> keeps form and adds field error")
    void updateLesson_policyFieldViolation_keepsFormAndAddsFieldError()
            throws Exception {

        var form =
                updateDto();

        var exception =
                new LessonUpdateException(
                        TEACHER_LESSON_ID,
                        form,
                        List.of(
                                new LessonPolicyViolation(
                                        "startTime",
                                        CREATE_POLICY_MESSAGE)));

        when(lessonService.update(
                TEACHER_ID,
                TEACHER_LESSON_ID,
                form))
                .thenThrow(
                        exception);

        var result =
                mockMvc.perform(
                        validUpdateRequest())
                        .andExpect(
                                status().isOk())
                        .andExpect(
                                view().name(
                                        "lessons/edit"))
                        .andExpect(
                                model().attribute(
                                        "form",
                                        form))
                        .andExpect(
                                model().attribute(
                                        "lesson",
                                        lesson()))
                        .andExpect(
                                model().attributeHasFieldErrors(
                                        "form",
                                        "startTime"))
                        .andReturn();

        assertFieldError(
                result,
                "startTime",
                CREATE_POLICY_MESSAGE);

        verify(lessonService)
                .update(
                        TEACHER_ID,
                        TEACHER_LESSON_ID,
                        form);

        verify(pageDataLoader)
                .addPageData(
                        any(Model.class),
                        eq(TEACHER_ID),
                        eq(TEACHER_LESSON_ID),
                        eq(form));
    }

    @Test
    @DisplayName("POST /lessons/{id}/edit global policy violation -> keeps form and adds page error")
    void updateLesson_globalPolicyViolation_keepsFormAndAddsPageError()
            throws Exception {

        var form =
                updateDto();

        var exception =
                new LessonUpdateException(
                        TEACHER_LESSON_ID,
                        form,
                        List.of(
                                new LessonPolicyViolation(
                                        null,
                                        UPDATE_GLOBAL_POLICY_MESSAGE)));

        when(lessonService.update(
                TEACHER_ID,
                TEACHER_LESSON_ID,
                form))
                .thenThrow(
                        exception);

        mockMvc.perform(
                validUpdateRequest())
                .andExpect(
                        status().isOk())
                .andExpect(
                        view().name(
                                "lessons/edit"))
                .andExpect(
                        model().attribute(
                                "form",
                                form))
                .andExpect(
                        model().attribute(
                                "lesson",
                                lesson()))
                .andExpect(
                        model().attribute(
                                "err",
                                UPDATE_GLOBAL_POLICY_MESSAGE));

        verify(lessonService)
                .update(
                        TEACHER_ID,
                        TEACHER_LESSON_ID,
                        form);

        verify(pageDataLoader)
                .addPageData(
                        any(Model.class),
                        eq(TEACHER_ID),
                        eq(TEACHER_LESSON_ID),
                        eq(form));
    }

    @Test
    @DisplayName("POST /lessons/{id}/edit schedule conflicts -> adds time and room field errors")
    void updateLesson_scheduleConflicts_addsFieldErrors()
            throws Exception {

        var form =
                updateDto();

        var start =
                UPDATE_DATE.atTime(
                        UPDATE_START_TIME)
                        .atZone(
                                clock.getZone())
                        .toOffsetDateTime();

        var end =
                UPDATE_DATE.atTime(
                        UPDATE_END_TIME)
                        .atZone(
                                clock.getZone())
                        .toOffsetDateTime();

        var requested =
                new RequestedSlot(
                        GROUP_ID,
                        COURSE_ID,
                        TEACHER_ID,
                        start,
                        end,
                        ROOM,
                        LessonType.LECTURE,
                        UPDATE_DESCRIPTION);

        var conflict =
                new OverlapHit(
                        STUDENT_LESSON_ID,
                        GROUP_ID,
                        COURSE_ID,
                        TEACHER_ID,
                        start,
                        end,
                        ROOM);

        var exception =
                new ScheduleConflictException(
                        TEACHER_LESSON_ID,
                        requested,
                        List.of(
                                conflict),
                        List.of(
                                conflict),
                        List.of(
                                conflict));

        when(lessonService.update(
                TEACHER_ID,
                TEACHER_LESSON_ID,
                form))
                .thenThrow(
                        exception);

        var result =
                mockMvc.perform(
                        validUpdateRequest())
                        .andExpect(
                                status().isOk())
                        .andExpect(
                                view().name(
                                        "lessons/edit"))
                        .andExpect(
                                model().attribute(
                                        "form",
                                        form))
                        .andExpect(
                                model().attribute(
                                        "lesson",
                                        lesson()))
                        .andExpect(
                                model().attribute(
                                        "schedule_conflict",
                                        exception))
                        .andExpect(
                                model().attributeHasFieldErrors(
                                        "form",
                                        "startTime",
                                        "room"))
                        .andReturn();

        assertFieldErrorsContain(
                result,
                "startTime",
                "The study group already has another lesson during this time.",
                "You already have another lesson during this time.");

        assertFieldError(
                result,
                "room",
                "Selected room is already occupied during this time.");

        verify(lessonService)
                .update(
                        TEACHER_ID,
                        TEACHER_LESSON_ID,
                        form);

        verify(pageDataLoader)
                .addPageData(
                        any(Model.class),
                        eq(TEACHER_ID),
                        eq(TEACHER_LESSON_ID),
                        eq(form));
    }
}