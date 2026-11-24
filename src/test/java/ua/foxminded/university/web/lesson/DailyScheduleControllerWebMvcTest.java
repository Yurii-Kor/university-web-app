package ua.foxminded.university.web.lesson;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static ua.foxminded.university.web.lesson.ScheduleControllerWebMvcTestConstants.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ua.foxminded.university.TestClockConfig;
import ua.foxminded.university.model.domain.enums.LessonType;
import ua.foxminded.university.model.persistence.lesson.projection.LessonScheduleItemView;
import ua.foxminded.university.model.persistence.studygroup.projection.StudentGroupHeaderView;
import ua.foxminded.university.service.lesson.LessonService;
import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.lesson.export.ScheduleExportDataFactory;
import ua.foxminded.university.web.lesson.export.pdf.SchedulePdfGenerator;
import ua.foxminded.university.web.lesson.page.DailySchedulePageModel;
import ua.foxminded.university.web.lesson.page.DailySchedulePageModelFactory;
import ua.foxminded.university.web.lesson.page.WeeklySchedulePageModelFactory;
import ua.foxminded.university.web.lesson.page.strategy.StudentSchedulePageStrategy;
import ua.foxminded.university.web.lesson.page.strategy.TeacherSchedulePageStrategy;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;
import ua.foxminded.university.web.util.PrincipalHandler;

@WebMvcTest(controllers = ScheduleController.class)
@Import({
        MethodSecurityTestConfig.class,
        ExceptionMessageReader.class,
        TestClockConfig.class,
        DailySchedulePageModelFactory.class,
        TeacherSchedulePageStrategy.class,
        StudentSchedulePageStrategy.class
})
class DailyScheduleControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WeeklySchedulePageModelFactory weeklyPageFactory;
    
    @MockitoBean
    private ScheduleExportDataFactory exportDataFactory;

    @MockitoBean
    private SchedulePdfGenerator pdfGenerator;

    @MockitoBean
    private LessonService lessonService;

    @MockitoBean
    private StudyGroupService groupService;

    @MockitoBean
    private TeacherService teacherService;

    @MockitoBean
    private PrincipalHandler principalHandler;

    @Test
    @DisplayName("GET /schedule/day as teacher -> shows own daily schedule")
    void getDailySchedule_teacher_showsOwnSchedule()
            throws Exception {

        var lesson =
                lesson();

        when(principalHandler.getRole(
                any()))
                .thenReturn(
                        "teacher");

        when(principalHandler.parseUserId(
                any()))
                .thenReturn(
                        TEACHER_ID);

        when(lessonService.listTeacherScheduleItems(
                TEACHER_ID,
                DAY_FROM,
                DAY_TO))
                .thenReturn(
                        List.of(
                                lesson));

        var result =
                mockMvc.perform(
                        get("/schedule/day")
                                .param(
                                        "date",
                                        REQUESTED_DATE.toString())
                                .with(user(
                                        Long.toString(
                                                TEACHER_ID))
                                        .roles(
                                                "TEACHER")))
                        .andExpect(
                                status().isOk())
                        .andExpect(
                                view().name(
                                        "lessons/day"))
                        .andExpect(
                                model().attributeExists(
                                        "page"))
                        .andReturn();

        var page =
                extractPage(
                        result);

        assertAll(
                () -> assertEquals(
                        REQUESTED_DATE,
                        page.date()),

                () -> assertTrue(
                        page.today()),

                () -> assertFalse(
                        page.showTeacherInfo()),

                () -> assertTrue(
                        page.showGroupInfo()),

                () -> assertTrue(
                        page.allowManage()),

                () -> assertEquals(
                        List.of(
                                lesson),
                        page.lessons()));

        verify(lessonService)
                .listTeacherScheduleItems(
                        TEACHER_ID,
                        DAY_FROM,
                        DAY_TO);

        verify(
                groupService,
                never())
                .getGroupHeaderForStudent(
                        anyLong());

        verify(
                lessonService,
                never())
                .listGroupScheduleItems(
                        anyLong(),
                        any(),
                        any());
    }

    @Test
    @DisplayName("GET /schedule/day as student -> shows own group daily schedule")
    void getDailySchedule_student_showsOwnGroupSchedule()
            throws Exception {

        var group =
                new StudentGroupHeaderView(
                        GROUP_ID,
                        GROUP_NAME,
                        GROUP_STUDENT_COUNT);

        var lesson =
                lesson();

        when(principalHandler.getRole(
                any()))
                .thenReturn(
                        "student");

        when(principalHandler.parseUserId(
                any()))
                .thenReturn(
                        STUDENT_ID);

        when(groupService.getGroupHeaderForStudent(
                STUDENT_ID))
                .thenReturn(
                        group);

        when(lessonService.listGroupScheduleItems(
                GROUP_ID,
                DAY_FROM,
                DAY_TO))
                .thenReturn(
                        List.of(
                                lesson));

        var result =
                mockMvc.perform(
                        get("/schedule/day")
                                .param(
                                        "date",
                                        REQUESTED_DATE.toString())
                                .with(user(
                                        Long.toString(
                                                STUDENT_ID))
                                        .roles(
                                                "STUDENT")))
                        .andExpect(
                                status().isOk())
                        .andExpect(
                                view().name(
                                        "lessons/day"))
                        .andExpect(
                                model().attributeExists(
                                        "page"))
                        .andReturn();

        var page =
                extractPage(
                        result);

        assertAll(
                () -> assertEquals(
                        REQUESTED_DATE,
                        page.date()),

                () -> assertTrue(
                        page.showTeacherInfo()),

                () -> assertFalse(
                        page.showGroupInfo()),

                () -> assertFalse(
                        page.allowManage()),

                () -> assertEquals(
                        List.of(
                                lesson),
                        page.lessons()));

        verify(groupService)
                .getGroupHeaderForStudent(
                        STUDENT_ID);

        verify(lessonService)
                .listGroupScheduleItems(
                        GROUP_ID,
                        DAY_FROM,
                        DAY_TO);

        verify(
                lessonService,
                never())
                .listTeacherScheduleItems(
                        anyLong(),
                        any(),
                        any());
    }

    @Test
    @DisplayName("GET /schedule/day as admin -> 403, schedule data not loaded")
    void getDailySchedule_admin_forbidden()
            throws Exception {

        mockMvc.perform(
                get("/schedule/day")
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
                groupService);
    }

    private DailySchedulePageModel extractPage(
            MvcResult result) {

        var model =
                result.getModelAndView()
                        .getModel();

        assertTrue(
                model.containsKey(
                        "page"));

        return (DailySchedulePageModel) model.get(
                "page");
    }

    private LessonScheduleItemView lesson() {

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
}