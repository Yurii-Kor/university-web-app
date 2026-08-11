package ua.foxminded.university.service.studygroup;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.studygroup.StudyGroupServiceTestConstants.*;

import java.util.Arrays;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import ua.foxminded.university.TestcontainersConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
	scripts = "/sql/studygroup/study-group-service-base.sql",
	executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
	scripts = "/sql/cleanup-database.sql",
	executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class StudyGroupServiceQueryTest {

	@Autowired
	private StudyGroupService groupService;

	@Test
	@DisplayName("findByIds: null/empty -> returns empty")
	void findByIds_nullOrEmpty_returnsEmpty() {
		assertTrue(
				groupService.findByIds(null)
						.isEmpty());

		assertTrue(
				groupService.findByIds(List.of())
						.isEmpty());
	}

	@Test
	@DisplayName("findByIds: nulls and duplicates -> returns unique non-null groups")
	void findByIds_nullsAndDuplicates_returnsUniqueGroups() {
		var ids = Arrays.asList(
				null,
				GROUP_CS_ID,
				GROUP_CS_ID,
				null);

		var groups = groupService.findByIds(ids);

		assertEquals(
				ONE_GROUP,
				groups.size());

		assertEquals(
				GROUP_CS_ID,
				groups.getFirst().getId());
	}

	@Test
	@DisplayName("listGroupCardsForAdmin: returns all groups with student and course counts")
	void listGroupCardsForAdmin_returnsAllGroupsWithCounts() {
		var page = groupService.listGroupCardsForAdmin(
				PageRequest.of(0, 10));

		assertNotNull(page);

		assertEquals(
				4,
				page.getTotalElements());

		var csCard = page.getContent()
				.stream()
				.filter(card -> card.id().equals(
						GROUP_CS_ID))
				.findFirst()
				.orElseThrow();

		assertEquals(
				GROUP_CS,
				csCard.name());

		assertEquals(
				2L,
				csCard.studentCount());

		assertEquals(
				3L,
				csCard.courseCount());

		var seCard = page.getContent()
				.stream()
				.filter(card -> card.id().equals(
						GROUP_SE_ID))
				.findFirst()
				.orElseThrow();

		assertEquals(
				0L,
				seCard.studentCount(),
				"soft-deleted student must not be counted");

		assertEquals(
				1L,
				seCard.courseCount());
	}

	@Test
	@DisplayName("listGroupCardsForTeacher: returns only groups where teacher has courses")
	void listGroupCardsForTeacher_returnsTeacherGroups() {
		var page = groupService.listGroupCardsForTeacher(
				TEACHER_1_ID,
				PageRequest.of(0, 10));

		assertEquals(
				2,
				page.getTotalElements());

		var ids = page.getContent()
				.stream()
				.map(card -> card.id())
				.toList();

		assertTrue(
				ids.contains(GROUP_CS_ID));

		assertTrue(
				ids.contains(GROUP_SE_ID));

		assertFalse(
				ids.contains(GROUP_EE_ID));

		assertFalse(
				ids.contains(GROUP_ME_ID));

		var csCard = page.getContent()
				.stream()
				.filter(card -> card.id().equals(
						GROUP_CS_ID))
				.findFirst()
				.orElseThrow();

		assertEquals(
				2L,
				csCard.studentCount());

		assertEquals(
				2L,
				csCard.courseCount(),
				"teacher must see count of own courses in the group");
	}

	@Test
	@DisplayName("listGroupCardsForTeacher: second teacher returns only own group")
	void listGroupCardsForTeacher_secondTeacher_returnsOwnGroup() {
		var page = groupService.listGroupCardsForTeacher(
				TEACHER_2_ID,
				PageRequest.of(0, 10));

		assertEquals(
				ONE_GROUP,
				page.getTotalElements());

		var card = page.getContent()
				.getFirst();

		assertEquals(
				GROUP_CS_ID,
				card.id());

		assertEquals(
				1L,
				card.courseCount());
	}

	@Test
	@DisplayName("listGroupCardsForTeacher: missing teacher returns empty page")
	void listGroupCardsForTeacher_missingTeacher_returnsEmpty() {
		var page = groupService.listGroupCardsForTeacher(
				MISSING_ID,
				PageRequest.of(0, 10));

		assertNotNull(page);
		assertTrue(page.isEmpty());
	}

	@Test
	@DisplayName("getGroupHeaderForStudent: returns student's group header")
	void getGroupHeaderForStudent_existingStudent_returnsHeader() {
		var header = groupService.getGroupHeaderForStudent(
				STUDENT_1_ID);

		assertNotNull(header);

		assertEquals(
				GROUP_CS_ID,
				header.id());

		assertEquals(
				GROUP_CS,
				header.name());

		assertEquals(
				2L,
				header.studentCount());
	}

	@Test
	@DisplayName("getGroupHeaderForStudent: missing student -> EntityNotFoundException")
	void getGroupHeaderForStudent_missingStudent_fails() {
		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> groupService.getGroupHeaderForStudent(
						MISSING_ID));

		assertEquals(
				"StudyGroup not found for student: id="
						+ MISSING_ID,
				exception.getMessage());
	}

	@Test
	@DisplayName("listGroupOptions: returns all groups ordered by name")
	void listGroupOptions_returnsAllOrdered() {
		var options = groupService.listGroupOptions();

		assertEquals(
				4,
				options.size());

		assertEquals(
				List.of(
						GROUP_CS,
						GROUP_EE,
						GROUP_ME,
						GROUP_SE),
				options.stream()
						.map(option -> option.name())
						.toList());
	}
}