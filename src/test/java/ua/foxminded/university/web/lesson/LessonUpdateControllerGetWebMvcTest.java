package ua.foxminded.university.web.lesson;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static ua.foxminded.university.web.lesson.ScheduleControllerWebMvcTestConstants.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.ui.Model;

import ua.foxminded.university.TestClockConfig;
import ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView;
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
class LessonUpdateControllerGetWebMvcTest
        extends AbstractLessonUpdateControllerWebMvcTest {

    @Test
    @DisplayName("GET /lessons/{id}/edit as teacher -> shows edit form for own lesson")
    void getEditLesson_teacher_showsEditForm()
            throws Exception {

        var lesson =
                lesson();

        var form =
                existingLessonDto();

        whenLessonExists(
                lesson,
                form);

        mockMvc.perform(
                get(
                        "/lessons/{id}/edit",
                        TEACHER_LESSON_ID)
                        .with(user(
                                Long.toString(
                                        TEACHER_ID))
                                .roles(
                                        "TEACHER")))
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
                                lesson));

        verify(lessonService)
                .getTeacherScheduleItem(
                        TEACHER_ID,
                        TEACHER_LESSON_ID);

        verify(pageDataLoader)
                .toForm(
                        lesson);

        verify(pageDataLoader)
                .addPageData(
                        any(Model.class),
                        eq(TEACHER_ID),
                        eq(lesson),
                        eq(form));
    }

    @Test
    @DisplayName("GET /lessons/{id}/edit as student -> 403")
    void getEditLesson_student_forbidden()
            throws Exception {

        mockMvc.perform(
                get(
                        "/lessons/{id}/edit",
                        TEACHER_LESSON_ID)
                        .with(user(
                                Long.toString(
                                        STUDENT_ID))
                                .roles(
                                        "STUDENT")))
                .andExpect(
                        status().isForbidden());

        verifyNoInteractions(
                principalHandler,
                lessonService,
                pageDataLoader);
    }

    @Test
    @DisplayName("GET /lessons/{id}/edit as admin -> 403")
    void getEditLesson_admin_forbidden()
            throws Exception {

        mockMvc.perform(
                get(
                        "/lessons/{id}/edit",
                        TEACHER_LESSON_ID)
                        .with(user(
                                Long.toString(
                                        ADMIN_ID))
                                .roles(
                                        "ADMIN")))
                .andExpect(
                        status().isForbidden());

        verifyNoInteractions(
                principalHandler,
                lessonService,
                pageDataLoader);
    }

    @Test
    @DisplayName("GET /lessons/{id}/edit unauthenticated -> 401")
    void getEditLesson_unauthenticated_unauthorized()
            throws Exception {

        mockMvc.perform(
                get(
                        "/lessons/{id}/edit",
                        TEACHER_LESSON_ID))
                .andExpect(
                        status().isUnauthorized());

        verifyNoInteractions(
                principalHandler,
                lessonService,
                pageDataLoader);
    }

    private void whenLessonExists(
            LessonScheduleItemView lesson,
            LessonUpdateDto form) {

        when(lessonService.getTeacherScheduleItem(
                TEACHER_ID,
                TEACHER_LESSON_ID))
                .thenReturn(
                        lesson);

        when(pageDataLoader.toForm(
                lesson))
                .thenReturn(
                        form);
    }
}