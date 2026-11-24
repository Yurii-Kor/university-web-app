package ua.foxminded.university.web.group;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ua.foxminded.university.service.studygroup.StudyGroupService;
import ua.foxminded.university.service.studygroup.exception.StudyGroupDeleteException;
import ua.foxminded.university.service.teacher.TeacherService;
import ua.foxminded.university.web.group.cards.GroupsCardsPageModelFactory;
import ua.foxminded.university.web.testconfig.MethodSecurityTestConfig;
import ua.foxminded.university.web.util.ExceptionMessageReader;
import ua.foxminded.university.web.util.PrincipalHandler;

@WebMvcTest(controllers = GroupsManagementController.class)
@Import({
	GroupsManagementExceptionHandler.class,
	MethodSecurityTestConfig.class,
	ExceptionMessageReader.class
})
class GroupsManagementDeleteWebMvcTest {

	private static final long ADMIN_ID = 41L;
	private static final long TEACHER_ID = 42L;
	private static final long STUDENT_ID = 43L;

	private static final long GROUP_ID = 1001L;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private StudyGroupService groupService;

	@MockitoBean
	private GroupsCardsPageModelFactory pageFactory;

	@MockitoBean
	private TeacherService teacherService;

	@MockitoBean
	private PrincipalHandler principalHandler;

	@Test
	@DisplayName("POST /groups/{id}/delete as admin -> redirects to same page, flash ok, calls service")
	void deleteGroup_admin_ok()
			throws Exception {

		doNothing()
				.when(groupService)
				.delete(GROUP_ID);

		mockMvc.perform(
				post("/groups/{id}/delete", GROUP_ID)
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param("page", "2"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(
						"/groups?page=2"))
				.andExpect(flash().attribute(
						"ok",
						"Group deleted."));

		verify(groupService)
				.delete(GROUP_ID);
	}

	@Test
	@DisplayName("POST /groups/{id}/delete when group has students -> redirects, flash err/groupId")
	void deleteGroup_withStudents_redirectsWithCardError()
			throws Exception {

		var studentCount = 3L;

		var message =
				"Group cannot be deleted because it has active students.";

		var ex = new StudyGroupDeleteException(
				GROUP_ID,
				studentCount,
				message);

		doThrow(ex)
				.when(groupService)
				.delete(GROUP_ID);

		mockMvc.perform(
				post("/groups/{id}/delete", GROUP_ID)
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param("page", "0"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(
						"/groups"))
				.andExpect(flash().attribute(
						"err",
						message))
				.andExpect(flash().attribute(
						"groupId",
						GROUP_ID));

		verify(groupService)
				.delete(GROUP_ID);
	}

	@Test
	@DisplayName("POST /groups/{id}/delete when group not found -> redirects, flash err")
	void deleteGroup_notFound_redirectsWithError()
			throws Exception {

		var message =
				"Study group not found: id=" + GROUP_ID;

		doThrow(new EntityNotFoundException(
				message))
				.when(groupService)
				.delete(GROUP_ID);

		mockMvc.perform(
				post("/groups/{id}/delete", GROUP_ID)
						.with(user(
								Long.toString(ADMIN_ID))
								.roles("ADMIN"))
						.with(csrf())
						.param("page", "0"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl(
						"/groups"))
				.andExpect(flash().attribute(
						"err",
						message));

		verify(groupService)
				.delete(GROUP_ID);
	}

	@Test
	@DisplayName("POST /groups/{id}/delete as teacher -> 403, service not called")
	void deleteGroup_teacher_forbidden()
	        throws Exception {

	    mockMvc.perform(
	            post("/groups/{id}/delete", GROUP_ID)
	                    .with(user(
	                            Long.toString(TEACHER_ID))
	                            .roles("TEACHER"))
	                    .with(csrf()))
	            .andExpect(status().isForbidden());

	    verify(groupService, never())
	            .delete(anyLong());
	}

	@Test
	@DisplayName("POST /groups/{id}/delete as student -> 403, service not called")
	void deleteGroup_student_forbidden()
	        throws Exception {

	    mockMvc.perform(
	            post("/groups/{id}/delete", GROUP_ID)
	                    .with(user(
	                            Long.toString(STUDENT_ID))
	                            .roles("STUDENT"))
	                    .with(csrf()))
	            .andExpect(status().isForbidden());

	    verify(groupService, never())
	            .delete(anyLong());
	}

	@Test
	@DisplayName("POST /groups/{id}/delete unauthenticated -> 401, service not called")
	void deleteGroup_unauthenticated_unauthorized()
			throws Exception {

		mockMvc.perform(
				post("/groups/{id}/delete", GROUP_ID)
						.with(csrf()))
				.andExpect(status().isUnauthorized());

		verify(groupService, never())
				.delete(anyLong());
	}
}