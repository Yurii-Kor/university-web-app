package ua.foxminded.university.model.persistence.course;

import static org.junit.jupiter.api.Assertions.*;

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
		statements = {
				"""
				insert into app_user (
					id,
					email,
					password,
					role,
					first_name,
					last_name,
					enabled
				)
				values (
					1001,
					'ada.teacher@example.com',
					'Abcd1234!',
					'TEACHER',
					'Ada',
					'Lovelace',
					true
				);
				""",
				"""
				insert into app_user (
					id,
					email,
					password,
					role,
					first_name,
					last_name,
					enabled
				)
				values (
					1002,
					'alan.teacher@example.com',
					'Abcd1234!',
					'TEACHER',
					'Alan',
					'Turing',
					true
				);
				""",
				"""
				insert into app_user (
					id,
					email,
					password,
					role,
					first_name,
					last_name,
					enabled
				)
				values (
					1003,
					'grace.student@example.com',
					'Abcd1234!',
					'STUDENT',
					'Grace',
					'Hopper',
					true
				);
				""",
				"""
				insert into teacher (
					id,
					academic_rank,
					office
				)
				values (
					1001,
					'PROFESSOR',
					'A-101'
				);
				""",
				"""
				insert into teacher (
					id,
					academic_rank,
					office
				)
				values (
					1002,
					'LECTURER',
					'B-202'
				);
				""",
				"""
				insert into groups (
					id,
					name
				)
				values (
					2001,
					'CS-201'
				);
				""",
				"""
				insert into groups (
					id,
					name
				)
				values (
					2002,
					'CS-202'
				);
				""",
				"""
				insert into student (
					id,
					group_id,
					enrollment_year
				)
				values (
					1003,
					2001,
					2024
				);
				""",
				"""
				insert into courses (
					id,
					code,
					name,
					description,
					teacher_id
				)
				values (
					3001,
					'CSE-B-200',
					'Operating Systems',
					'Operating systems course',
					1001
				);
				""",
				"""
				insert into courses (
					id,
					code,
					name,
					description,
					teacher_id
				)
				values (
					3002,
					'CSE-C-300',
					'Databases',
					null,
					1001
				);
				""",
				"""
				insert into courses (
					id,
					code,
					name,
					description,
					teacher_id
				)
				values (
					3003,
					'CSE-A-100',
					'Algorithms',
					'Algorithms course',
					1002
				);
				""",
				"""
				insert into group_courses (
					group_id,
					course_id
				)
				values (
					2001,
					3001
				);
				""",
				"""
				insert into group_courses (
					group_id,
					course_id
				)
				values (
					2001,
					3003
				);
				""",
				"""
				insert into group_courses (
					group_id,
					course_id
				)
				values (
					2002,
					3002
				);
				"""
		},
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class CourseRepositoryTest {

	static final long FIRST_TEACHER_ID = 1001L;
	static final long SECOND_TEACHER_ID = 1002L;
	static final long STUDENT_ID = 1003L;

	static final long OPERATING_SYSTEMS_COURSE_ID = 3001L;
	static final long DATABASES_COURSE_ID = 3002L;
	static final long ALGORITHMS_COURSE_ID = 3003L;

	static final long MISSING_ID = 999_999L;

	@Autowired
	private CourseRepository courseRepository;

	@Test
	@DisplayName("findCourseCardsAll: returns all projections ordered by code with correct page metadata")
	void findCourseCardsAll_returnsOrderedPage() {
		var page = courseRepository.findCourseCardsAll(
				PageRequest.of(0, 2));

		assertNotNull(page);
		assertEquals(3, page.getTotalElements());
		assertEquals(2, page.getTotalPages());
		assertEquals(2, page.getNumberOfElements());
		assertTrue(page.hasNext());

		var cards = page.getContent();

		assertEquals(ALGORITHMS_COURSE_ID, cards.get(0).id());
		assertEquals("CSE-A-100", cards.get(0).code());
		assertEquals("Algorithms", cards.get(0).name());

		assertEquals(OPERATING_SYSTEMS_COURSE_ID, cards.get(1).id());
		assertEquals("CSE-B-200", cards.get(1).code());
		assertEquals("Operating Systems", cards.get(1).name());
	}

	@Test
	@DisplayName("findCourseCardsAll: maps course and teacher fields into projection")
	void findCourseCardsAll_mapsProjectionFields() {
		var page = courseRepository.findCourseCardsAll(
				PageRequest.of(0, 10));

		var algorithmCard = page.getContent()
				.stream()
				.filter(card -> card.id().equals(ALGORITHMS_COURSE_ID))
				.findFirst()
				.orElseThrow();

		assertEquals("CSE-A-100", algorithmCard.code());
		assertEquals("Algorithms", algorithmCard.name());
		assertEquals("Algorithms course", algorithmCard.description());

		assertEquals(
				SECOND_TEACHER_ID,
				algorithmCard.teacherId());

		assertEquals(
				"alan.teacher@example.com",
				algorithmCard.teacherEmail());

		assertEquals(
				"Alan",
				algorithmCard.teacherFirstName());

		assertEquals(
				"Turing",
				algorithmCard.teacherLastName());
	}

	@Test
	@DisplayName("findCourseCardsByTeacherId: returns only selected teacher courses ordered by code")
	void findCourseCardsByTeacherId_returnsOnlyTeacherCourses() {
		var firstPage = courseRepository.findCourseCardsByTeacherId(
				FIRST_TEACHER_ID,
				PageRequest.of(0, 1));

		assertEquals(2, firstPage.getTotalElements());
		assertEquals(2, firstPage.getTotalPages());
		assertEquals(1, firstPage.getNumberOfElements());
		assertTrue(firstPage.hasNext());

		var firstCard = firstPage.getContent().getFirst();

		assertEquals(
				OPERATING_SYSTEMS_COURSE_ID,
				firstCard.id());

		assertEquals("CSE-B-200", firstCard.code());

		assertEquals(
				FIRST_TEACHER_ID,
				firstCard.teacherId());

		var secondPage = courseRepository.findCourseCardsByTeacherId(
				FIRST_TEACHER_ID,
				PageRequest.of(1, 1));

		assertEquals(2, secondPage.getTotalElements());
		assertEquals(1, secondPage.getNumberOfElements());
		assertFalse(secondPage.hasNext());

		var secondCard = secondPage.getContent().getFirst();

		assertEquals(
				DATABASES_COURSE_ID,
				secondCard.id());

		assertEquals("CSE-C-300", secondCard.code());

		assertEquals(
				FIRST_TEACHER_ID,
				secondCard.teacherId());
	}

	@Test
	@DisplayName("findCourseCardsByTeacherId: missing teacher returns empty page")
	void findCourseCardsByTeacherId_missingTeacher_returnsEmptyPage() {
		var page = courseRepository.findCourseCardsByTeacherId(
				MISSING_ID,
				PageRequest.of(0, 10));

		assertNotNull(page);
		assertTrue(page.isEmpty());
		assertEquals(0, page.getTotalElements());
		assertEquals(0, page.getTotalPages());
	}

	@Test
	@DisplayName("findCourseCardsByStudentId: returns courses assigned to student's group ordered by code")
	void findCourseCardsByStudentId_returnsGroupCourses() {
		var firstPage = courseRepository.findCourseCardsByStudentId(
				STUDENT_ID,
				PageRequest.of(0, 1));

		assertEquals(2, firstPage.getTotalElements());
		assertEquals(2, firstPage.getTotalPages());
		assertEquals(1, firstPage.getNumberOfElements());
		assertTrue(firstPage.hasNext());

		var firstCard = firstPage.getContent().getFirst();

		assertEquals(
				ALGORITHMS_COURSE_ID,
				firstCard.id());

		assertEquals("CSE-A-100", firstCard.code());

		var secondPage = courseRepository.findCourseCardsByStudentId(
				STUDENT_ID,
				PageRequest.of(1, 1));

		assertEquals(2, secondPage.getTotalElements());
		assertEquals(1, secondPage.getNumberOfElements());
		assertFalse(secondPage.hasNext());

		var secondCard = secondPage.getContent().getFirst();

		assertEquals(
				OPERATING_SYSTEMS_COURSE_ID,
				secondCard.id());

		assertEquals("CSE-B-200", secondCard.code());
	}

	@Test
	@DisplayName("findCourseCardsByStudentId: excludes courses of other groups")
	void findCourseCardsByStudentId_excludesOtherGroupCourses() {
		var page = courseRepository.findCourseCardsByStudentId(
				STUDENT_ID,
				PageRequest.of(0, 10));

		var courseIds = page.getContent()
				.stream()
				.map(card -> card.id())
				.toList();

		assertEquals(2, courseIds.size());

		assertTrue(
				courseIds.contains(ALGORITHMS_COURSE_ID));

		assertTrue(
				courseIds.contains(OPERATING_SYSTEMS_COURSE_ID));

		assertFalse(
				courseIds.contains(DATABASES_COURSE_ID));
	}

	@Test
	@DisplayName("findCourseCardsByStudentId: missing student returns empty page")
	void findCourseCardsByStudentId_missingStudent_returnsEmptyPage() {
		var page = courseRepository.findCourseCardsByStudentId(
				MISSING_ID,
				PageRequest.of(0, 10));

		assertNotNull(page);
		assertTrue(page.isEmpty());
		assertEquals(0, page.getTotalElements());
		assertEquals(0, page.getTotalPages());
	}

	@Test
	@DisplayName("findCourseHeaderById: existing course returns header projection")
	void findCourseHeaderById_existingCourse_returnsProjection() {
		var header = courseRepository.findCourseHeaderById(
				OPERATING_SYSTEMS_COURSE_ID);

		assertTrue(header.isPresent());

		var courseHeader = header.orElseThrow();

		assertEquals(
				OPERATING_SYSTEMS_COURSE_ID,
				courseHeader.id());

		assertEquals(
				"CSE-B-200",
				courseHeader.code());

		assertEquals(
				"Operating Systems",
				courseHeader.name());
	}

	@Test
	@DisplayName("findCourseHeaderById: missing course returns Optional.empty")
	void findCourseHeaderById_missingCourse_returnsEmpty() {
		var header = courseRepository.findCourseHeaderById(
				MISSING_ID);

		assertTrue(header.isEmpty());
	}
}