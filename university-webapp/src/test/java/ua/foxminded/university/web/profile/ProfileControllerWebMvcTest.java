package ua.foxminded.university.web.profile;

import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.OffsetDateTime;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.repository.dto.AdminProfileView;
import ua.foxminded.university.model.repository.dto.StudentProfileView;
import ua.foxminded.university.model.repository.dto.TeacherProfileView;
import ua.foxminded.university.service.AppUserService;
import ua.foxminded.university.service.StudentService;
import ua.foxminded.university.service.TeacherService;
import ua.foxminded.university.service.dto.request.appuser.AppUserSelfUpdateDto;
import ua.foxminded.university.web.profile.page.ProfilePageModelFactory;
import ua.foxminded.university.web.profile.page.strategy.AdminProfilePageStrategy;
import ua.foxminded.university.web.profile.page.strategy.StudentProfilePageStrategy;
import ua.foxminded.university.web.profile.page.strategy.TeacherProfilePageStrategy;
import ua.foxminded.university.web.util.ExceptionMessageReader;
import ua.foxminded.university.web.util.PrincipalHandler;

@WebMvcTest(controllers = ProfileController.class)
@Import({ 
	ProfileExceptionHandler.class, 
	PrincipalHandler.class,
	ProfilePageModelFactory.class,
	AdminProfilePageStrategy.class,
	StudentProfilePageStrategy.class,
	TeacherProfilePageStrategy.class,
	ExceptionMessageReader.class
})
class ProfileControllerWebMvcTest {
	
	private final static Long DEFAULT_ID = 42L;

	@Autowired MockMvc mockMvc;

	@MockitoBean AppUserService appUserService;
	@MockitoBean StudentService studentService;
	@MockitoBean TeacherService teacherService;

	@Test
	@DisplayName("GET /profile as ADMIN -> 200, profile/admin, model has AdminProfileView")
	void getProfile_admin_ok_returnsAdminViewAndModel() throws Exception {
		var profileView = new AdminProfileView(
				"admin@example.com", 
				"Alice", 
				"Admin",
				OffsetDateTime.parse("2025-01-01T12:00Z"));

		when(appUserService.getAdminProfileView(DEFAULT_ID)).thenReturn(profileView);

		mockMvc.perform(get("/profile").with(user(Long.toString(DEFAULT_ID)).roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(view().name("profile/admin"))
				.andExpect(model().attribute("profile", sameInstance(profileView)));
	}

	@Test
	@DisplayName("GET /profile as TEACHER -> 200, profile/teacher, model has TeacherProfileView")
	void getProfile_teacher_ok_returnsTeacherViewAndModel() throws Exception {
		var profileView = new TeacherProfileView(
				"teacher@example.com", 
				"Alice", 
				"Smith",
				OffsetDateTime.parse("2025-01-01T12:00Z"), 
				AcademicRank.PROFESSOR, 
				"B-101");

		when(teacherService.getTeacherProfileView(DEFAULT_ID)).thenReturn(profileView);

		mockMvc.perform(get("/profile").with(user(Long.toString(DEFAULT_ID)).roles("TEACHER")))
				.andExpect(status().isOk())
				.andExpect(view().name("profile/teacher"))
				.andExpect(model().attribute("profile", sameInstance(profileView)));
	}

	@Test
	@DisplayName("GET /profile as STUDENT -> 200, profile/student, model has StudentProfileView")
	void getProfile_student_ok_returnsStudentViewAndModel() throws Exception {
		var profileView = new StudentProfileView(
				"student@example.com", 
				"John", 
				"Doe",
				OffsetDateTime.parse("2025-01-01T12:00Z"), 
				2024, 
				"CS-101");

		when(studentService.getStudentProfileView(DEFAULT_ID)).thenReturn(profileView);

		mockMvc.perform(get("/profile").with(user(Long.toString(DEFAULT_ID)).roles("STUDENT")))
				.andExpect(status().isOk())
				.andExpect(view().name("profile/student"))
				.andExpect(model().attribute("profile", sameInstance(profileView)));
	}
	
	@Test
	@DisplayName("POST /profile/self/update -> redirects /profile, flash updated=true, calls service with normalized dto")
	void postUpdateSelf_ok_redirectsAndSetsUpdatedFlashAndCallsService() throws Exception {
		mockMvc.perform(post("/profile/self/update").with(user(Long.toString(DEFAULT_ID)).roles("TEACHER"))
				.with(csrf())
				.param("email", "a@b.com")
				.param("firstName", "John")
				.param("lastName", "Doe"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/profile"))
				.andExpect(flash().attribute("updated", true));

		verify(appUserService).updateProfileFields(new AppUserSelfUpdateDto(DEFAULT_ID, "a@b.com", "John", "Doe"));
	}

	@Test
	@DisplayName("POST /profile/self/update when IllegalArgumentException -> redirects /profile, flash err=message")
	void postUpdateSelf_illegalArgument_redirectsProfileAndSetsErrFlash() throws Exception {
		doThrow(new IllegalArgumentException("Invalid input")).when(appUserService)
				.updateProfileFields(any(AppUserSelfUpdateDto.class));

		mockMvc.perform(post("/profile/self/update").with(user(Long.toString(DEFAULT_ID)).roles("TEACHER"))
				.with(csrf())
				.param("email", "a@b.com")
				.param("firstName", "John")
				.param("lastName", "Doe"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/profile"))
				.andExpect(flash().attribute("err", "Invalid input"));
	}

	@Test
	@DisplayName("POST /profile/self/update when EntityNotFoundException -> redirects /login, flash err=fixed message")
	void postUpdateSelf_entityNotFound_redirectsLoginAndSetsErrFlash() throws Exception {
		doThrow(new EntityNotFoundException("not found")).when(appUserService)
				.updateProfileFields(any(AppUserSelfUpdateDto.class));

		mockMvc.perform(post("/profile/self/update").with(user(Long.toString(DEFAULT_ID)).roles("TEACHER"))
				.with(csrf())
				.param("email", "a@b.com")
				.param("firstName", "John")
				.param("lastName", "Doe"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"))
				.andExpect(flash().attribute("err", "User not found. Please log in again."));
	}

	@Test
	@DisplayName("GET /profile when runtime exception -> redirects /profile, flash err=generic")
	void getProfile_runtimeException_redirectsProfileAndSetsGenericErrFlash() throws Exception {
		when(teacherService.getTeacherProfileView(DEFAULT_ID)).thenThrow(new RuntimeException("boom"));

		mockMvc.perform(get("/profile").with(user(Long.toString(DEFAULT_ID)).roles("TEACHER")))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/profile"))
				.andExpect(flash().attribute("err", "Something went wrong."));
	}
}
