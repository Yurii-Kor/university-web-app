package ua.foxminded.university.service.course;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.service.course.CourseServiceTestConstants.*;

import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import ua.foxminded.university.TestcontainersConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql(
	scripts = "/sql/course/course-service-base.sql",
	executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
@Sql(
	scripts = "/sql/cleanup-database.sql",
	executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
class CourseServiceGroupsTest {

	@Autowired
	private CourseService courseService;

	@Test
	@DisplayName("addGroupToCourse: adds group when not attached -> returns Optional.of(groupId) and attaches")
	void addGroupToCourse_adds_whenNotAttached() {
		var added = courseService.addGroupToCourse(
				COURSE_SECURITY_ID,
				GROUP_2_ID);

		assertTrue(
				added.isPresent(),
				"must return groupId when added");

		assertEquals(
				GROUP_2_ID,
				added.get());

		var groupsView = courseService.getCourseGroupsView(
				COURSE_SECURITY_ID);

		var assignedGroupIds = groupsView.assigned()
				.stream()
				.map(group -> group.id())
				.toList();

		assertTrue(
				assignedGroupIds.containsAll(
						List.of(GROUP_1_ID, GROUP_2_ID)),
				"course must reference both groups");

		assertTrue(
				groupsView.available().isEmpty(),
				"no groups must remain available");
	}

	@Test
	@DisplayName("addGroupToCourse: group already attached -> returns Optional.empty and changes nothing")
	void addGroupToCourse_alreadyAttached_returnsEmpty() {
		var added = courseService.addGroupToCourse(
				COURSE_SECURITY_ID,
				GROUP_1_ID);

		assertTrue(
				added.isEmpty(),
				"must be empty when already attached");

		var groupsView = courseService.getCourseGroupsView(
				COURSE_SECURITY_ID);

		var assignedGroupIds = groupsView.assigned()
				.stream()
				.map(group -> group.id())
				.toList();

		assertEquals(
				List.of(GROUP_1_ID),
				assignedGroupIds,
				"course must still have only the originally assigned group");

		var availableGroupIds = groupsView.available()
				.stream()
				.map(group -> group.id())
				.toList();

		assertEquals(
				List.of(GROUP_2_ID),
				availableGroupIds,
				"second group must remain available");
	}

	@Test
	@DisplayName("addGroupToCourse: non-existing course -> EntityNotFoundException")
	void addGroupToCourse_courseNotFound_fails() {
		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> courseService.addGroupToCourse(
						NON_EXISTENT_ID,
						GROUP_1_ID));

		assertEquals(
				"Course not found: id=" + NON_EXISTENT_ID,
				exception.getMessage());
	}

	@Test
	@DisplayName("removeGroupFromCourse: removes attached group -> returns Optional.of(groupId) and detaches")
	void removeGroupFromCourse_removes_whenAttached() {
		var removed = courseService.removeGroupFromCourse(
				COURSE_OS_ID,
				GROUP_1_ID);

		assertTrue(
				removed.isPresent(),
				"must return groupId when removed");

		assertEquals(
				GROUP_1_ID,
				removed.get());

		var groupsView = courseService.getCourseGroupsView(
				COURSE_OS_ID);

		var assignedGroupIds = groupsView.assigned()
				.stream()
				.map(group -> group.id())
				.toList();

		assertFalse(
				assignedGroupIds.contains(GROUP_1_ID),
				"g1 must be removed");

		assertTrue(
				assignedGroupIds.contains(GROUP_2_ID),
				"g2 must remain assigned");

		var availableGroupIds = groupsView.available()
				.stream()
				.map(group -> group.id())
				.toList();

		assertTrue(
				availableGroupIds.contains(GROUP_1_ID),
				"removed group must become available");
	}

	@Test
	@DisplayName("removeGroupFromCourse: group not attached -> returns Optional.empty and changes nothing")
	void removeGroupFromCourse_notAttached_returnsEmpty() {
		var removed = courseService.removeGroupFromCourse(
				COURSE_SECURITY_ID,
				GROUP_2_ID);

		assertTrue(
				removed.isEmpty(),
				"must be empty when group is not attached");

		var groupsView = courseService.getCourseGroupsView(
				COURSE_SECURITY_ID);

		var assignedGroupIds = groupsView.assigned()
				.stream()
				.map(group -> group.id())
				.toList();

		assertEquals(
				List.of(GROUP_1_ID),
				assignedGroupIds,
				"only g1 must remain assigned");

		var availableGroupIds = groupsView.available()
				.stream()
				.map(group -> group.id())
				.toList();

		assertEquals(
				List.of(GROUP_2_ID),
				availableGroupIds,
				"g2 must remain available");
	}

	@Test
	@DisplayName("removeGroupFromCourse: non-existing course -> EntityNotFoundException")
	void removeGroupFromCourse_courseNotFound_fails() {
		var exception = assertThrows(
				EntityNotFoundException.class,
				() -> courseService.removeGroupFromCourse(
						NON_EXISTENT_ID,
						GROUP_1_ID));

		assertEquals(
				"Course not found: id=" + NON_EXISTENT_ID,
				exception.getMessage());
	}
}