package ua.foxminded.university.web.lesson;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.foxminded.university.web.lesson.ScheduleControllerWebMvcTestConstants.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ua.foxminded.university.TestClockConfig;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.lesson.export.ScheduleExportData;
import ua.foxminded.university.web.lesson.export.ScheduleExportDataFactory;
import ua.foxminded.university.web.lesson.export.pdf.SchedulePdfGenerator;
import ua.foxminded.university.web.lesson.page.DailySchedulePageModelFactory;
import ua.foxminded.university.web.lesson.page.WeeklySchedulePageModel;
import ua.foxminded.university.web.lesson.page.WeeklySchedulePageModelFactory;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@WebMvcTest(controllers = ScheduleController.class)
@Import({
		MethodSecurityTestConfig.class,
		ExceptionMessageReader.class,
		TestClockConfig.class
})
class ScheduleExportControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private WeeklySchedulePageModelFactory weeklyPageFactory;

	@MockitoBean
	private DailySchedulePageModelFactory dailyPageFactory;

	@MockitoBean
	private ScheduleExportDataFactory exportDataFactory;

	@MockitoBean
	private SchedulePdfGenerator pdfGenerator;

	@MockitoBean
	private TeacherService teacherService;

	@Test
	@DisplayName("GET /schedule/export as teacher -> returns PDF attachment")
	void exportSchedule_teacher_returnsPdfAttachment()
			throws Exception {

		var weekStart =
				WEEK_FROM.toLocalDate();

		var weekEnd =
				WEEK_TO.minusDays(
						1)
						.toLocalDate();

		var schedule =
				new WeeklySchedulePageModel(
						"My schedule",
						"Your teaching schedule.",
						weekStart,
						weekEnd,
						List.of(),
						false,
						true,
						true);

		var exportData =
				new ScheduleExportData(
						"Teacher",
						"Alice Teacher",
						List.of(
								"Academic rank: PROFESSOR",
								"Office: A-101"),
						schedule);

		var pdf =
				"%PDF-1.7\nmock-pdf"
						.getBytes(
								StandardCharsets.US_ASCII);

		when(exportDataFactory.build(
				any(),
				eq(
						REQUESTED_DATE)))
				.thenReturn(
						exportData);

		when(pdfGenerator.generate(
				exportData))
				.thenReturn(
						pdf);

		var fileName =
				"schedule-%s-%s.pdf"
						.formatted(
								weekStart,
								weekEnd);

		mockMvc.perform(
				get(
						"/schedule/export")
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
						content().contentType(
								MediaType.APPLICATION_PDF))
				.andExpect(
						header().string(
								HttpHeaders.CONTENT_DISPOSITION,
								containsString(
										"attachment")))
				.andExpect(
						header().string(
								HttpHeaders.CONTENT_DISPOSITION,
								containsString(
										fileName)))
				.andExpect(
						header().longValue(
								HttpHeaders.CONTENT_LENGTH,
								pdf.length))
				.andExpect(
						content().bytes(
								pdf));

		verify(exportDataFactory)
				.build(
						any(),
						eq(
								REQUESTED_DATE));

		verify(pdfGenerator)
				.generate(
						exportData);
	}

	@Test
	@DisplayName("GET /schedule/export as admin -> 403")
	void exportSchedule_admin_forbidden()
			throws Exception {

		mockMvc.perform(
				get(
						"/schedule/export")
						.param(
								"date",
								REQUESTED_DATE.toString())
						.with(user(
								Long.toString(
										ADMIN_ID))
								.roles(
										"ADMIN")))
				.andExpect(
						status().isForbidden());

		verifyNoInteractions(
				exportDataFactory,
				pdfGenerator);
	}

	@Test
	@DisplayName("GET /schedule/export unauthenticated -> 401")
	void exportSchedule_unauthenticated_unauthorized()
			throws Exception {

		mockMvc.perform(
				get(
						"/schedule/export")
						.param(
								"date",
								REQUESTED_DATE.toString()))
				.andExpect(
						status().isUnauthorized());

		verifyNoInteractions(
				exportDataFactory,
				pdfGenerator);
	}
}