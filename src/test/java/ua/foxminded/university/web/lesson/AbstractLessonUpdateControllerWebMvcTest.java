package ua.foxminded.university.web.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ua.foxminded.university.web.lesson.ScheduleControllerWebMvcTestConstants.*;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView;
import ua.foxminded.university.service.lesson.LessonService;
import ua.foxminded.university.service.lesson.dto.LessonUpdateDto;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.util.PrincipalHandler;

abstract class AbstractLessonUpdateControllerWebMvcTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected Clock clock;

    @MockitoBean
    protected LessonService lessonService;

    @MockitoBean
    protected LessonUpdatePageDataLoader pageDataLoader;

    @MockitoBean
    protected PrincipalHandler principalHandler;

    @MockitoBean
    protected TeacherService teacherService;

    @BeforeEach
    void setUpLessonUpdateControllerTest() {

        when(principalHandler.parseUserId(
                any()))
                .thenReturn(
                        TEACHER_ID);

        stubPageDataLoader();
    }

    protected LessonScheduleItemView lesson() {

        return new LessonScheduleItemView(
                TEACHER_LESSON_ID,
                COURSE_ID,
                COURSE_CODE,
                COURSE_NAME,
                GROUP_ID,
                GROUP_NAME,
                TEACHER_ID,
                TEACHER_FIRST_NAME,
                TEACHER_LAST_NAME,
                TEACHER_LESSON_START,
                TEACHER_LESSON_END,
                ROOM,
                LessonType.LECTURE,
                DAILY_LESSON_DESCRIPTION);
    }

    protected LessonUpdateDto existingLessonDto() {

        return new LessonUpdateDto(
                TEACHER_LESSON_START
                        .atZoneSameInstant(
                                clock.getZone())
                        .toLocalDate(),
                TEACHER_LESSON_START
                        .atZoneSameInstant(
                                clock.getZone())
                        .toLocalTime(),
                TEACHER_LESSON_END
                        .atZoneSameInstant(
                                clock.getZone())
                        .toLocalTime(),
                ROOM,
                LessonType.LECTURE,
                DAILY_LESSON_DESCRIPTION);
    }

    protected LessonUpdateDto updateDto() {

        return new LessonUpdateDto(
                UPDATE_DATE,
                UPDATE_START_TIME,
                UPDATE_END_TIME,
                ROOM,
                LessonType.LECTURE,
                UPDATE_DESCRIPTION);
    }

    protected MockHttpServletRequestBuilder validUpdateRequest() {

        return post(
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
                        ROOM)
                .param(
                        "lessonType",
                        LessonType.LECTURE.name())
                .param(
                        "description",
                        UPDATE_DESCRIPTION);
    }

    protected void assertFieldError(
            MvcResult result,
            String field,
            String expectedMessage) {

        var bindingResult =
                extractBindingResult(
                        result);

        var fieldError =
                bindingResult.getFieldError(
                        field);

        assertNotNull(
                fieldError);

        assertEquals(
                expectedMessage,
                fieldError.getDefaultMessage());
    }

    protected void assertFieldErrorsContain(
            MvcResult result,
            String field,
            String... expectedMessages) {

        var bindingResult =
                extractBindingResult(
                        result);

        var actualMessages =
                bindingResult.getFieldErrors(
                        field)
                        .stream()
                        .map(error ->
                                error.getDefaultMessage())
                        .toList();

        for (var expectedMessage : expectedMessages) {

            assertTrue(
                    actualMessages.contains(
                            expectedMessage),
                    () ->
                            "Expected field '%s' to contain error '%s', but errors were %s"
                                    .formatted(
                                            field,
                                            expectedMessage,
                                            actualMessages));
        }
    }

    private void stubPageDataLoader() {

        doAnswer(invocation -> {

            var model =
                    invocation.<Model>getArgument(
                            0);

            var currentLesson =
                    invocation.<LessonScheduleItemView>getArgument(
                            2);

            var form =
                    invocation.<LessonUpdateDto>getArgument(
                            3);

            addEditPageAttributes(
                    model,
                    currentLesson,
                    form);

            return null;

        }).when(pageDataLoader)
                .addPageData(
                        any(Model.class),
                        eq(TEACHER_ID),
                        any(LessonScheduleItemView.class),
                        any(LessonUpdateDto.class));

        doAnswer(invocation -> {

            var model =
                    invocation.<Model>getArgument(
                            0);

            var form =
                    invocation.<LessonUpdateDto>getArgument(
                            3);

            addEditPageAttributes(
                    model,
                    lesson(),
                    form);

            return null;

        }).when(pageDataLoader)
                .addPageData(
                        any(Model.class),
                        eq(TEACHER_ID),
                        anyLong(),
                        any(LessonUpdateDto.class));
    }

    private void addEditPageAttributes(
            Model model,
            LessonScheduleItemView lesson,
            LessonUpdateDto form) {

        model.addAttribute(
                "lesson",
                lesson);

        model.addAttribute(
                "lesson_types",
                LessonType.values());

        model.addAttribute(
                "today",
                LocalDate.now(
                        clock));

        model.addAttribute(
                "selected_date",
                form.date());

        model.addAttribute(
                "lesson_date",
                lesson.startTime()
                        .atZoneSameInstant(
                                clock.getZone())
                        .toLocalDate());

        model.addAttribute(
                "teacher_schedule",
                List.of());

        model.addAttribute(
                "group_schedule",
                List.of());
    }

    private BindingResult extractBindingResult(
            MvcResult result) {

        return assertInstanceOf(
                BindingResult.class,
                result.getModelAndView()
                        .getModel()
                        .get(
                                BindingResult.MODEL_KEY_PREFIX
                                        + "form"));
    }
}