package ua.foxminded.university.web.lesson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static ua.foxminded.university.web.lesson.ScheduleControllerWebMvcTestConstants.*;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import ua.foxminded.university.TestClockConfig;
import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.model.persistence.lesson.projection.OverlapHit;
import ua.foxminded.university.service.lesson.LessonPolicyViolation;
import ua.foxminded.university.service.lesson.LessonService;
import ua.foxminded.university.service.lesson.dto.LessonCreateDto;
import ua.foxminded.university.service.lesson.exception.LessonCreateException;
import ua.foxminded.university.service.lesson.exception.RequestedSlot;
import ua.foxminded.university.service.lesson.exception.ScheduleConflictException;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;
import ua.foxminded.university.web.util.PrincipalHandler;

@WebMvcTest(controllers = LessonCreateController.class)
@Import({
        LessonCreateExceptionHandler.class,
        MethodSecurityTestConfig.class,
        ExceptionMessageReader.class,
        TestClockConfig.class
})
class LessonCreateControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Clock clock;

    @MockitoBean
    private LessonService lessonService;

    @MockitoBean
    private LessonCreatePageDataLoader pageDataLoader;

    @MockitoBean
    private PrincipalHandler principalHandler;

    @MockitoBean
    private TeacherService teacherService;

    @BeforeEach
    void setUp() {
        when(principalHandler.parseUserId(any()))
                .thenReturn(TEACHER_ID);
    }

    @Test
    @DisplayName("GET /lessons/create as teacher -> shows empty create form for today")
    void getCreateLesson_teacher_showsEmptyForm()
            throws Exception {

        var result = mockMvc.perform(
                get("/lessons/create")
                        .with(user(Long.toString(TEACHER_ID))
                                .roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("lessons/create"))
                .andExpect(model().attributeExists("form"))
                .andReturn();

        var form = assertInstanceOf(
                LessonCreateDto.class,
                result.getModelAndView()
                        .getModel()
                        .get("form"));

        assertNull(form.courseId());
        assertNull(form.groupId());
        assertEquals(LocalDate.now(clock), form.date());
        assertNull(form.startTime());
        assertNull(form.endTime());
        assertNull(form.room());
        assertNull(form.lessonType());
        assertNull(form.description());

        verify(pageDataLoader)
                .addPageData(
                        any(Model.class),
                        eq(TEACHER_ID),
                        eq(form));

        verifyNoInteractions(lessonService);
    }

    @Test
    @DisplayName("GET /lessons/create with context -> keeps course, group and date")
    void getCreateLesson_withContext_keepsSelection()
            throws Exception {

        var expected = new LessonCreateDto(
                COURSE_ID,
                GROUP_ID,
                CREATE_DATE,
                null,
                null,
                null,
                null,
                null);

        mockMvc.perform(
                get("/lessons/create")
                        .param("courseId", Long.toString(COURSE_ID))
                        .param("groupId", Long.toString(GROUP_ID))
                        .param("date", CREATE_DATE.toString())
                        .with(user(Long.toString(TEACHER_ID))
                                .roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("lessons/create"))
                .andExpect(model().attribute("form", expected));

        verify(pageDataLoader)
                .addPageData(
                        any(Model.class),
                        eq(TEACHER_ID),
                        eq(expected));

        verifyNoInteractions(lessonService);
    }

    @Test
    @DisplayName("POST /lessons/create valid form -> creates lesson and redirects to selected day")
    void createLesson_validForm_createsAndRedirects()
            throws Exception {

        var expected = createDto();

        mockMvc.perform(
                validCreateRequest())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/schedule/day?date=" + CREATE_DATE))
                .andExpect(flash().attribute(
                        "ok",
                        "Lesson created."));

        verify(lessonService)
                .create(
                        TEACHER_ID,
                        expected);

        verify(
                pageDataLoader,
                never())
                .addPageData(
                        any(Model.class),
                        any(Long.class),
                        any(LessonCreateDto.class));
    }

    @Test
    @DisplayName("POST /lessons/create invalid form -> validation handler keeps form and reloads page data")
    void createLesson_invalidForm_returnsCreateView()
            throws Exception {

        var result = mockMvc.perform(
                post("/lessons/create")
                        .with(user(Long.toString(TEACHER_ID))
                                .roles("TEACHER"))
                        .with(csrf())
                        .param("courseId", Long.toString(COURSE_ID))
                        .param("groupId", Long.toString(GROUP_ID))
                        .param("date", CREATE_DATE.toString())
                        .param("startTime", CREATE_START_TIME.toString())
                        .param("endTime", CREATE_END_TIME.toString())
                        .param("room", "")
                        .param("lessonType", LessonType.LECTURE.name())
                        .param("description", CREATE_DESCRIPTION))
                .andExpect(status().isOk())
                .andExpect(view().name("lessons/create"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attributeHasFieldErrors(
                        "form",
                        "room"))
                .andReturn();

        var form = assertInstanceOf(
                LessonCreateDto.class,
                result.getModelAndView()
                        .getModel()
                        .get("form"));

        assertEquals(COURSE_ID, form.courseId());
        assertEquals(GROUP_ID, form.groupId());
        assertEquals(CREATE_DATE, form.date());
        assertEquals(CREATE_START_TIME, form.startTime());
        assertEquals(CREATE_END_TIME, form.endTime());
        assertNull(form.room());

        verify(
                lessonService,
                never())
                .create(
                        any(Long.class),
                        any(LessonCreateDto.class));

        verify(pageDataLoader)
                .addPageData(
                        any(Model.class),
                        eq(TEACHER_ID),
                        eq(form));
    }

    @Test
    @DisplayName("POST /lessons/create policy violation -> keeps form and adds field error")
    void createLesson_policyViolation_keepsFormAndAddsFieldError()
            throws Exception {

        var form = createDto();

        var exception = new LessonCreateException(
                form,
                List.of(
                        new LessonPolicyViolation(
                                "startTime",
                                CREATE_POLICY_MESSAGE)));

        when(lessonService.create(
                TEACHER_ID,
                form))
                .thenThrow(exception);

        var result = mockMvc.perform(
                validCreateRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("lessons/create"))
                .andExpect(model().attribute(
                        "form",
                        form))
                .andExpect(model().attributeHasFieldErrors(
                        "form",
                        "startTime"))
                .andReturn();

        assertFieldError(
                result,
                "startTime",
                CREATE_POLICY_MESSAGE);

        verify(lessonService)
                .create(
                        TEACHER_ID,
                        form);

        verify(pageDataLoader)
                .addPageData(
                        any(Model.class),
                        eq(TEACHER_ID),
                        eq(form));
    }

    @Test
    @DisplayName("POST /lessons/create schedule conflicts -> adds group, teacher and room field errors")
    void createLesson_scheduleConflicts_addsFieldErrors()
            throws Exception {

        var form = createDto();

        var start = CREATE_DATE.atTime(CREATE_START_TIME)
                .atZone(clock.getZone())
                .toOffsetDateTime();

        var end = CREATE_DATE.atTime(CREATE_END_TIME)
                .atZone(clock.getZone())
                .toOffsetDateTime();

        var requested = new RequestedSlot(
                GROUP_ID,
                COURSE_ID,
                TEACHER_ID,
                start,
                end,
                ROOM,
                LessonType.LECTURE,
                CREATE_DESCRIPTION);

        var conflict = new OverlapHit(
                CREATE_LESSON_ID,
                GROUP_ID,
                COURSE_ID,
                TEACHER_ID,
                start,
                end,
                ROOM);

        var exception = new ScheduleConflictException(
                requested,
                List.of(conflict),
                List.of(conflict),
                List.of(conflict));

        when(lessonService.create(
                TEACHER_ID,
                form))
                .thenThrow(exception);

        var result = mockMvc.perform(
                validCreateRequest())
                .andExpect(status().isOk())
                .andExpect(view().name("lessons/create"))
                .andExpect(model().attribute(
                        "form",
                        form))
                .andExpect(model().attribute(
                        "schedule_conflict",
                        exception))
                .andExpect(model().attributeHasFieldErrors(
                        "form",
                        "groupId",
                        "startTime",
                        "room"))
                .andReturn();

        assertFieldError(
                result,
                "groupId",
                "Selected study group already has a lesson during this time.");

        assertFieldError(
                result,
                "startTime",
                "You already have another lesson during this time.");

        assertFieldError(
                result,
                "room",
                "Selected room is already occupied during this time.");

        verify(pageDataLoader)
                .addPageData(
                        any(Model.class),
                        eq(TEACHER_ID),
                        eq(form));
    }

    @Test
    @DisplayName("GET /lessons/create as student -> 403")
    void getCreateLesson_student_forbidden()
            throws Exception {

        mockMvc.perform(
                get("/lessons/create")
                        .with(user(Long.toString(STUDENT_ID))
                                .roles("STUDENT")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(
                principalHandler,
                lessonService,
                pageDataLoader);
    }

    @Test
    @DisplayName("GET /lessons/create as admin -> 403")
    void getCreateLesson_admin_forbidden()
            throws Exception {

        mockMvc.perform(
                get("/lessons/create")
                        .with(user(Long.toString(CREATE_ADMIN_ID))
                                .roles("ADMIN")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(
                principalHandler,
                lessonService,
                pageDataLoader);
    }

    @Test
    @DisplayName("GET /lessons/create unauthenticated -> 401")
    void getCreateLesson_unauthenticated_unauthorized()
            throws Exception {

        mockMvc.perform(
                get("/lessons/create"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(
                principalHandler,
                lessonService,
                pageDataLoader);
    }

    private LessonCreateDto createDto() {
        return new LessonCreateDto(
                COURSE_ID,
                GROUP_ID,
                CREATE_DATE,
                CREATE_START_TIME,
                CREATE_END_TIME,
                ROOM,
                LessonType.LECTURE,
                CREATE_DESCRIPTION);
    }

    private MockHttpServletRequestBuilder validCreateRequest() {
        return post("/lessons/create")
                .with(user(Long.toString(TEACHER_ID))
                        .roles("TEACHER"))
                .with(csrf())
                .param("courseId", Long.toString(COURSE_ID))
                .param("groupId", Long.toString(GROUP_ID))
                .param("date", CREATE_DATE.toString())
                .param("startTime", CREATE_START_TIME.toString())
                .param("endTime", CREATE_END_TIME.toString())
                .param("room", ROOM)
                .param("lessonType", LessonType.LECTURE.name())
                .param("description", CREATE_DESCRIPTION);
    }

    private void assertFieldError(
            MvcResult result,
            String field,
            String expectedMessage) {

        var bindingResult = assertInstanceOf(
                BindingResult.class,
                result.getModelAndView()
                        .getModel()
                        .get(BindingResult.MODEL_KEY_PREFIX + "form"));

        var fieldError = bindingResult.getFieldError(field);

        assertNotNull(fieldError);
        assertEquals(
                expectedMessage,
                fieldError.getDefaultMessage());
    }
}