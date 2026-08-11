package ua.foxminded.university.model.persistence.studygroup;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import ua.foxminded.university.TestcontainersConfiguration;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
		replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import(TestcontainersConfiguration.class)
@Sql(
		scripts = "/sql/studygroup/study-group-repository-base.sql",
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class StudyGroupRepositoryTest {

	static final long TEACHER_1_ID = 1001L;
	static final long TEACHER_2_ID = 1002L;

	static final long STUDENT_1_ID = 1003L;

	static final long GROUP_CS_ID = 2001L;
	static final long GROUP_SE_ID = 2002L;
	static final long GROUP_EE_ID = 2003L;
	static final long GROUP_ME_ID = 2004L;
	static final long GROUP_DELETED_ID = 2005L;

	static final long COURSE_OS_ID = 3001L;

	static final long MISSING_ID = 999_999L;

	static final String GROUP_CS = "CS-101";
	static final String GROUP_SE = "SE-205";
	static final String GROUP_EE = "EE-205";
	static final String GROUP_ME = "ME-101";

	@Autowired
	private StudyGroupRepository groupRepository;

	@Test
	@DisplayName("findExistingIds: returns only existing active group ids")
	void findExistingIds_returnsOnlyExistingActiveIds() {
		var result = groupRepository.findExistingIds(
				List.of(
						GROUP_CS_ID,
						GROUP_ME_ID,
						GROUP_DELETED_ID,
						MISSING_ID));

		assertEquals(
				Set.of(
						GROUP_CS_ID,
						GROUP_ME_ID),
				new HashSet<>(result));
	}

	@Test
	@DisplayName("countStudentsByGroupId: counts only active students")
	void countStudentsByGroupId_countsOnlyActiveStudents() {
		var count = groupRepository.countStudentsByGroupId(
				GROUP_CS_ID);

		assertEquals(
				2L,
				count);
	}

	@Test
	@DisplayName("countStudentsByGroupId: group with only soft-deleted students -> zero")
	void countStudentsByGroupId_onlySoftDeletedStudents_returnsZero() {
		var count = groupRepository.countStudentsByGroupId(
				GROUP_SE_ID);

		assertEquals(
				0L,
				count);
	}

	@Test
	@DisplayName("findGroupOptions: returns active groups ordered by name")
	void findGroupOptions_returnsActiveGroupsOrdered() {
		var result = groupRepository.findGroupOptions();

		assertEquals(
				4,
				result.size());

		assertEquals(
				List.of(
						GROUP_CS,
						GROUP_EE,
						GROUP_ME,
						GROUP_SE),
				result.stream()
						.map(group -> group.name())
						.toList());

		assertFalse(
				result.stream()
						.anyMatch(group ->
								group.id().equals(
										GROUP_DELETED_ID)));
	}

	@Test
	@DisplayName("findAssignedGroupOptions: returns groups assigned to course")
	void findAssignedGroupOptions_returnsAssignedGroups() {
		var result = groupRepository.findAssignedGroupOptions(
				COURSE_OS_ID);

		assertEquals(
				2,
				result.size());

		assertEquals(
				List.of(
						GROUP_CS_ID,
						GROUP_SE_ID),
				result.stream()
						.map(group -> group.id())
						.toList());
	}

	@Test
	@DisplayName("findAvailableGroupOptions: returns active groups not assigned to course")
	void findAvailableGroupOptions_returnsAvailableGroups() {
		var result = groupRepository.findAvailableGroupOptions(
				COURSE_OS_ID);

		assertEquals(
				2,
				result.size());

		assertEquals(
				List.of(
						GROUP_EE_ID,
						GROUP_ME_ID),
				result.stream()
						.map(group -> group.id())
						.toList());

		assertFalse(
				result.stream()
						.anyMatch(group ->
								group.id().equals(
										GROUP_DELETED_ID)));
	}

	@Test
	@DisplayName("findGroupCardsAll: returns active groups with student and course counts")
	void findGroupCardsAll_returnsCardsWithCounts() {
		var page = groupRepository.findGroupCardsAll(
				PageRequest.of(0, 10));

		assertEquals(
				4L,
				page.getTotalElements());

		var csCard = page.getContent()
				.stream()
				.filter(card ->
						card.id().equals(
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
				.filter(card ->
						card.id().equals(
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

		assertFalse(
				page.getContent()
						.stream()
						.anyMatch(card ->
								card.id().equals(
										GROUP_DELETED_ID)));
	}

	@Test
	@DisplayName("findGroupCardsAll: pageable returns correct totals")
	void findGroupCardsAll_pageable_returnsCorrectTotals() {
		var page = groupRepository.findGroupCardsAll(
				PageRequest.of(0, 2));

		assertEquals(
				2,
				page.getContent().size());

		assertEquals(
				4L,
				page.getTotalElements());

		assertEquals(
				2,
				page.getTotalPages());

		assertTrue(
				page.hasNext());
	}

	@Test
	@DisplayName("findGroupCardsByTeacherId: returns teacher groups with own course counts")
	void findGroupCardsByTeacherId_returnsTeacherGroupsWithCounts() {
		var page = groupRepository.findGroupCardsByTeacherId(
				TEACHER_1_ID,
				PageRequest.of(0, 10));

		assertEquals(
				2L,
				page.getTotalElements());

		assertEquals(
				Set.of(
						GROUP_CS_ID,
						GROUP_SE_ID),
				page.getContent()
						.stream()
						.map(card -> card.id())
						.collect(
								java.util.stream.Collectors.toSet()));

		var csCard = page.getContent()
				.stream()
				.filter(card ->
						card.id().equals(
								GROUP_CS_ID))
				.findFirst()
				.orElseThrow();

		assertEquals(
				2L,
				csCard.studentCount());

		assertEquals(
				2L,
				csCard.courseCount(),
				"course count must include only courses of requested teacher");

		var seCard = page.getContent()
				.stream()
				.filter(card ->
						card.id().equals(
								GROUP_SE_ID))
				.findFirst()
				.orElseThrow();

		assertEquals(
				0L,
				seCard.studentCount());

		assertEquals(
				1L,
				seCard.courseCount());
	}

	@Test
	@DisplayName("findGroupCardsByTeacherId: returns only second teacher groups")
	void findGroupCardsByTeacherId_secondTeacher_returnsOwnGroups() {
		var page = groupRepository.findGroupCardsByTeacherId(
				TEACHER_2_ID,
				PageRequest.of(0, 10));

		assertEquals(
				1L,
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
	@DisplayName("findGroupCardsByTeacherId: missing teacher -> empty page")
	void findGroupCardsByTeacherId_missingTeacher_returnsEmptyPage() {
		var page = groupRepository.findGroupCardsByTeacherId(
				MISSING_ID,
				PageRequest.of(0, 10));

		assertTrue(
				page.isEmpty());

		assertEquals(
				0L,
				page.getTotalElements());
	}

	@Test
	@DisplayName("findStudentGroupHeader: returns student's group with active student count")
	void findStudentGroupHeader_returnsGroupHeader() {
		var result = groupRepository.findStudentGroupHeader(
				STUDENT_1_ID);

		assertTrue(
				result.isPresent());

		var header = result.orElseThrow();

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
	@DisplayName("findStudentGroupHeader: missing student -> empty Optional")
	void findStudentGroupHeader_missingStudent_returnsEmpty() {
		var result = groupRepository.findStudentGroupHeader(
				MISSING_ID);

		assertTrue(
				result.isEmpty());
	}
}