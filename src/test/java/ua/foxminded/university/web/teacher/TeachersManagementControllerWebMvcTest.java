package ua.foxminded.university.web.teacher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static ua.foxminded.university.web.teacher.TeachersManagementWebMvcTestConstants.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.teacher.page.TeachersPageModel;
import ua.foxminded.university.web.teacher.page.TeachersPageModelFactory;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;

@WebMvcTest(controllers = TeachersManagementController.class)
@Import({
		MethodSecurityTestConfig.class,
		ExceptionMessageReader.class
})
class TeachersManagementControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TeacherService teacherService;

	@MockitoBean
	private TeachersPageModelFactory pageFactory;

	@Test
	@DisplayName("GET /teachers as authenticated user -> 200, teachers/teachers, model has page")
	void getTeachers_authenticated_ok()
			throws Exception {

		var page = mock(
				TeachersPageModel.class);

		when(pageFactory.build(
				any(),
				anyInt()))
				.thenReturn(page);

		mockMvc.perform(
				get("/teachers")
						.param(
								"page",
								"2")
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(view().name(
						"teachers/teachers"))
				.andExpect(model().attribute(
						"page",
						page));

		verify(pageFactory)
				.build(
						any(),
						org.mockito.ArgumentMatchers.eq(
								2));
	}

	@Test
	@DisplayName("GET /teachers unauthenticated -> 401, factory not called")
	void getTeachers_unauthenticated_unauthorized()
			throws Exception {

		mockMvc.perform(
				get("/teachers"))
				.andExpect(status().isUnauthorized());

		verify(
				pageFactory,
				never())
				.build(
						any(),
						anyInt());
	}
}