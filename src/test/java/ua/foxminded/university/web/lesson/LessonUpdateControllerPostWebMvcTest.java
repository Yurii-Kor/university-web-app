package ua.foxminded.university.web.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static ua.foxminded.university.web.lesson.ScheduleControllerWebMvcTestConstants.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.ui.Model;

import ua.foxminded.university.TestClockConfig;
import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.service.lesson.dto.LessonUpdateDto;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@WebMvcTest(controllers = LessonUpdateController.class)
@Import({
        LessonUpdateExceptionHandler.class,
        MethodSecurityTestConfig.class,
        ExceptionMessageReader.class,
        TestClockConfig.class
})
class LessonUpdateControllerPostWebMvcTest
        extends AbstractLessonUpdateControllerWebMvcTest {

    @Test
    @DisplayName("POST /lessons/{id}/edit valid form -> updates lesson and redirects to updated day")
    void updateLesson_validForm_updatesAndRedirects()
            throws Exception {

        var expected =
                updateDto();

        mockMvc.perform(
                validUpdateRequest())
                .andExpect(
                        status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/schedule/day?date="
                                        + UPDATE_DATE))
                .andExpect(
                        flash().attribute(
                                "ok",
                                "Lesson updated."));

        verify(lessonService)
                .update(
                        TEACHER_ID,
                        TEACHER_LESSON_ID,
                        expected);

        verifyNoInteractions(
                pageDataLoader);
    }

    @Test
    @DisplayName("POST /lessons/{id}/edit invalid form -> keeps form and reloads page data")
    void updateLesson_invalidForm_returnsEditView()
            throws Exception {

        var result =
                mockMvc.perform(
                        post(
                                "/lessons/{id}/edit",
                                TEACHER_LESSON_ID)
                                .with(user(
                                        Long.toString(
                                                TEACHER_ID))
                                        .roles(
                                                "TEACHER"))
                                .with(csrf())
                                .param(
                                        "date",
                                        UPDATE_DATE.toString())
                                .param(
                                        "startTime",
                                        UPDATE_START_TIME.toString())
                                .param(
                                        "endTime",
                                        UPDATE_END_TIME.toString())
                                .param(
                                        "room",
                                        "")
                                .param(
                                        "lessonType",
                                        LessonType.LECTURE.name())
                                .param(
                                        "description",
                                        UPDATE_DESCRIPTION))
                        .andExpect(
                                status().isOk())
                        .andExpect(
                                view().name(
                                        "lessons/edit"))
                        .andExpect(
                                model().attributeExists(
                                        "form"))
                        .andExpect(
                                model().attribute(
                                        "lesson",
                                        lesson()))
                        .andExpect(
                                model().attributeHasFieldErrors(
                                        "form",
                                        "room"))
                        .andReturn();

        var form =
                assertInstanceOf(
                        LessonUpdateDto.class,
                        result.getModelAndView()
                                .getModel()
                                .get("form"));

        assertEquals(
                UPDATE_DATE,
                form.date());

        assertEquals(
                UPDATE_START_TIME,
                form.startTime());

        assertEquals(
                UPDATE_END_TIME,
                form.endTime());

        assertNull(
                form.room());

        assertEquals(
                LessonType.LECTURE,
                form.lessonType());

        assertEquals(
                UPDATE_DESCRIPTION,
                form.description());

        verify(
                lessonService,
                never())
                .update(
                        anyLong(),
                        anyLong(),
                        any(LessonUpdateDto.class));

        verify(pageDataLoader)
                .addPageData(
                        any(Model.class),
                        eq(TEACHER_ID),
                        eq(TEACHER_LESSON_ID),
                        eq(form));
    }
}