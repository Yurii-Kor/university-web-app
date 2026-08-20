package ua.foxminded.university.model.persistence.student;

import static org.junit.jupiter.api.Assertions.*;
import static ua.foxminded.university.model.persistence.student.StudentRepositoryTestConstants.*;

import java.util.List;

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
        scripts = "/sql/student/student-repository-base.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
class StudentRepositoryRoleCardsTest {

    @Autowired
    private StudentRepository studentRepository;

    @Test
    @DisplayName("findStudentCardsForTeacher: returns enabled students from teacher groups without duplicates")
    void findStudentCardsForTeacher_returnsTeacherStudentsWithoutDuplicates() {
        var page = studentRepository.findStudentCardsForTeacher(
                TEACHER_1_ID,
                PageRequest.of(0, 10));

        assertEquals(
                3L,
                page.getTotalElements(),
                "multiple teacher courses in one group must not duplicate students");

        assertEquals(
                List.of(
                        STUDENT_2_ID,
                        STUDENT_1_ID,
                        STUDENT_3_ID),
                page.getContent()
                        .stream()
                        .map(card -> card.id())
                        .toList());

        assertFalse(
                page.getContent()
                        .stream()
                        .anyMatch(card ->
                                card.id().equals(
                                        STUDENT_BLOCKED_ID)));

        assertFalse(
                page.getContent()
                        .stream()
                        .anyMatch(card ->
                                card.id().equals(
                                        STUDENT_DELETED_ID)));

        assertFalse(
                page.getContent()
                        .stream()
                        .anyMatch(card ->
                                card.id().equals(
                                        STUDENT_4_ID)));

        var csStudent = page.getContent()
                .stream()
                .filter(card ->
                        card.id().equals(
                                STUDENT_1_ID))
                .findFirst()
                .orElseThrow();

        assertEquals(
                GROUP_CS,
                csStudent.groupName());

        var seStudent = page.getContent()
                .stream()
                .filter(card ->
                        card.id().equals(
                                STUDENT_3_ID))
                .findFirst()
                .orElseThrow();

        assertEquals(
                GROUP_SE,
                seStudent.groupName());
    }

    @Test
    @DisplayName("findStudentCardsForTeacher: second teacher returns students from own groups")
    void findStudentCardsForTeacher_secondTeacher_returnsOwnStudents() {
        var page = studentRepository.findStudentCardsForTeacher(
                TEACHER_2_ID,
                PageRequest.of(0, 10));

        assertEquals(
                1L,
                page.getTotalElements());

        var card = page.getContent()
                .getFirst();

        assertEquals(
                STUDENT_4_ID,
                card.id());

        assertEquals(
                GROUP_EE,
                card.groupName());
    }

    @Test
    @DisplayName("findStudentCardsForTeacher: missing teacher -> empty page")
    void findStudentCardsForTeacher_missingTeacher_returnsEmptyPage() {
        var page = studentRepository.findStudentCardsForTeacher(
                MISSING_ID,
                PageRequest.of(0, 10));

        assertTrue(
                page.isEmpty());

        assertEquals(
                0L,
                page.getTotalElements());
    }

    @Test
    @DisplayName("findStudentCardsForStudent: returns enabled groupmates excluding current student")
    void findStudentCardsForStudent_returnsEnabledPeersOnly() {
        var page = studentRepository.findStudentCardsForStudent(
                STUDENT_1_ID,
                PageRequest.of(0, 10));

        assertEquals(
                1L,
                page.getTotalElements());

        var peer = page.getContent()
                .getFirst();

        assertEquals(
                STUDENT_2_ID,
                peer.id());

        assertEquals(
                STUDENT_2_EMAIL,
                peer.email());

        assertFalse(
                page.getContent()
                        .stream()
                        .anyMatch(card ->
                                card.id().equals(
                                        STUDENT_1_ID)));

        assertFalse(
                page.getContent()
                        .stream()
                        .anyMatch(card ->
                                card.id().equals(
                                        STUDENT_BLOCKED_ID)));

        assertFalse(
                page.getContent()
                        .stream()
                        .anyMatch(card ->
                                card.id().equals(
                                        STUDENT_DELETED_ID)));
    }

    @Test
    @DisplayName("findStudentCardsForStudent: missing student -> empty page")
    void findStudentCardsForStudent_missingStudent_returnsEmptyPage() {
        var page = studentRepository.findStudentCardsForStudent(
                MISSING_ID,
                PageRequest.of(0, 10));

        assertTrue(
                page.isEmpty());

        assertEquals(
                0L,
                page.getTotalElements());
    }
}