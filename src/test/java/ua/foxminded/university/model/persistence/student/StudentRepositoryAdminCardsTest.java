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
class StudentRepositoryAdminCardsTest {

    @Autowired
    private StudentRepository studentRepository;

    @Test
    @DisplayName("findStudentCardsForAccountManagement: returns active students including disabled")
    void findStudentCardsForAccountManagement_returnsActiveStudentsIncludingDisabled() {
        var page = studentRepository.findStudentCardsForAccountManagement(
                PageRequest.of(0, 10));

        assertEquals(
                5L,
                page.getTotalElements());

        assertEquals(
                List.of(
                        STUDENT_2_ID,
                        STUDENT_4_ID,
                        STUDENT_1_ID,
                        STUDENT_BLOCKED_ID,
                        STUDENT_3_ID),
                page.getContent()
                        .stream()
                        .map(card -> card.id())
                        .toList());

        var blockedCard = page.getContent()
                .stream()
                .filter(card ->
                        card.id().equals(
                                STUDENT_BLOCKED_ID))
                .findFirst()
                .orElseThrow();

        assertFalse(
                blockedCard.enabled());

        assertFalse(
                page.getContent()
                        .stream()
                        .anyMatch(card ->
                                card.id().equals(
                                        STUDENT_DELETED_ID)));
    }

    @Test
    @DisplayName("findStudentCardsForAccountManagement: pageable returns correct totals")
    void findStudentCardsForAccountManagement_pageable_returnsCorrectTotals() {
        var page = studentRepository.findStudentCardsForAccountManagement(
                PageRequest.of(0, 2));

        assertEquals(
                2,
                page.getContent().size());

        assertEquals(
                5L,
                page.getTotalElements());

        assertEquals(
                3,
                page.getTotalPages());

        assertTrue(
                page.hasNext());
    }

    @Test
    @DisplayName("findStudentCardsForAdmin: returns only active enabled students")
    void findStudentCardsForAdmin_returnsEnabledStudentsOnly() {
        var page = studentRepository.findStudentCardsForAdmin(
                PageRequest.of(0, 10));

        assertEquals(
                4L,
                page.getTotalElements());

        assertEquals(
                List.of(
                        STUDENT_2_ID,
                        STUDENT_4_ID,
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

        var card = page.getContent()
                .stream()
                .filter(student ->
                        student.id().equals(
                                STUDENT_1_ID))
                .findFirst()
                .orElseThrow();

        assertEquals(
                STUDENT_1_EMAIL,
                card.email());

        assertEquals(
                STUDENT_1_FIRST_NAME,
                card.firstName());

        assertEquals(
                STUDENT_1_LAST_NAME,
                card.lastName());

        assertEquals(
                GROUP_CS_ID,
                card.groupId());

        assertEquals(
                GROUP_CS,
                card.groupName());

        assertEquals(
                STUDENT_1_ENROLLMENT_YEAR,
                card.enrollmentYear());
    }
}