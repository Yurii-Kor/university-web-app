package ua.foxminded.university.web.lesson;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.foxminded.university.web.lesson.ScheduleControllerWebMvcTestConstants.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.persistence.EntityNotFoundException;
import ua.foxminded.university.service.lesson.LessonService;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;
import ua.foxminded.university.web.util.PrincipalHandler;

@WebMvcTest(controllers = LessonDeleteController.class)
@Import({
        LessonDeleteExceptionHandler.class,
        MethodSecurityTestConfig.class,
        ExceptionMessageReader.class
})
class LessonDeleteControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LessonService lessonService;

    @MockitoBean
    private PrincipalHandler principalHandler;

    @MockitoBean
    private TeacherService teacherService;

    @BeforeEach
    void setUp() {

        when(principalHandler.parseUserId(
                any()))
                .thenReturn(
                        TEACHER_ID);
    }

    @Test
    @DisplayName("POST /lessons/{id}/delete as teacher -> deletes lesson and redirects to same day")
    void deleteLesson_teacher_deletesAndRedirects()
            throws Exception {

        mockMvc.perform(
                post(
                        "/lessons/{id}/delete",
                        TEACHER_LESSON_ID)
                        .with(user(
                                Long.toString(
                                        TEACHER_ID))
                                .roles(
                                        "TEACHER"))
                        .with(csrf())
                        .param(
                                "date",
                                REQUESTED_DATE.toString()))
                .andExpect(
                        status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/schedule/day?date="
                                        + REQUESTED_DATE))
                .andExpect(
                        flash().attribute(
                                "ok",
                                "Lesson deleted."));

        verify(principalHandler)
                .parseUserId(
                        any());

        verify(lessonService)
                .deleteById(
                        TEACHER_ID,
                        TEACHER_LESSON_ID);
    }

    @Test
    @DisplayName("POST /lessons/{id}/delete when lesson not found -> redirects to same day with error")
    void deleteLesson_notFound_redirectsWithError()
            throws Exception {

        var message =
                "Lesson not found: id="
                        + TEACHER_LESSON_ID;

        doThrow(
                new EntityNotFoundException(
                        message))
                .when(lessonService)
                .deleteById(
                        TEACHER_ID,
                        TEACHER_LESSON_ID);

        mockMvc.perform(
                post(
                        "/lessons/{id}/delete",
                        TEACHER_LESSON_ID)
                        .with(user(
                                Long.toString(
                                        TEACHER_ID))
                                .roles(
                                        "TEACHER"))
                        .with(csrf())
                        .param(
                                "date",
                                REQUESTED_DATE.toString()))
                .andExpect(
                        status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/schedule/day?date="
                                        + REQUESTED_DATE))
                .andExpect(
                        flash().attribute(
                                "err",
                                message));

        verify(lessonService)
                .deleteById(
                        TEACHER_ID,
                        TEACHER_LESSON_ID);
    }

    @Test
    @DisplayName("POST /lessons/{id}/delete when lesson cannot be modified -> redirects to same day with error")
    void deleteLesson_cannotModify_redirectsWithError()
            throws Exception {

        var message =
                "Only future lessons can be modified.";

        doThrow(
                new IllegalStateException(
                        message))
                .when(lessonService)
                .deleteById(
                        TEACHER_ID,
                        TEACHER_LESSON_ID);

        mockMvc.perform(
                post(
                        "/lessons/{id}/delete",
                        TEACHER_LESSON_ID)
                        .with(user(
                                Long.toString(
                                        TEACHER_ID))
                                .roles(
                                        "TEACHER"))
                        .with(csrf())
                        .param(
                                "date",
                                REQUESTED_DATE.toString()))
                .andExpect(
                        status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/schedule/day?date="
                                        + REQUESTED_DATE))
                .andExpect(
                        flash().attribute(
                                "err",
                                message));

        verify(lessonService)
                .deleteById(
                        TEACHER_ID,
                        TEACHER_LESSON_ID);
    }

    @Test
    @DisplayName("POST /lessons/{id}/delete without CSRF -> 403, lesson not deleted")
    void deleteLesson_withoutCsrf_forbidden()
            throws Exception {

        mockMvc.perform(
                post(
                        "/lessons/{id}/delete",
                        TEACHER_LESSON_ID)
                        .with(user(
                                Long.toString(
                                        TEACHER_ID))
                                .roles(
                                        "TEACHER"))
                        .param(
                                "date",
                                REQUESTED_DATE.toString()))
                .andExpect(
                        status().isForbidden());

        verifyNoInteractions(
                principalHandler,
                lessonService);
    }

    @Test
    @DisplayName("POST /lessons/{id}/delete as student -> 403, lesson not deleted")
    void deleteLesson_student_forbidden()
            throws Exception {

        mockMvc.perform(
                post(
                        "/lessons/{id}/delete",
                        TEACHER_LESSON_ID)
                        .with(user(
                                Long.toString(
                                        STUDENT_ID))
                                .roles(
                                        "STUDENT"))
                        .with(csrf())
                        .param(
                                "date",
                                REQUESTED_DATE.toString()))
                .andExpect(
                        status().isForbidden());

        verifyNoInteractions(
                principalHandler,
                lessonService);
    }

    @Test
    @DisplayName("POST /lessons/{id}/delete as admin -> 403, lesson not deleted")
    void deleteLesson_admin_forbidden()
            throws Exception {

        mockMvc.perform(
                post(
                        "/lessons/{id}/delete",
                        TEACHER_LESSON_ID)
                        .with(user(
                                Long.toString(
                                        ADMIN_ID))
                                .roles(
                                        "ADMIN"))
                        .with(csrf())
                        .param(
                                "date",
                                REQUESTED_DATE.toString()))
                .andExpect(
                        status().isForbidden());

        verifyNoInteractions(
                principalHandler,
                lessonService);
    }

    @Test
    @DisplayName("POST /lessons/{id}/delete unauthenticated -> 401, lesson not deleted")
    void deleteLesson_unauthenticated_unauthorized()
            throws Exception {

        mockMvc.perform(
                post(
                        "/lessons/{id}/delete",
                        TEACHER_LESSON_ID)
                        .with(csrf())
                        .param(
                                "date",
                                REQUESTED_DATE.toString()))
                .andExpect(
                        status().isUnauthorized());

        verifyNoInteractions(
                principalHandler,
                lessonService);
    }
}