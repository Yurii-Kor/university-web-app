package ua.foxminded.university.web.student.cards;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ua.foxminded.university.model.persistence.student.projection.StudentCardProjection;

public interface StudentsCardsPageStrategy<T extends StudentCardProjection> {

    StudentsCardsPageMode mode();

    Page<T> loadStudents(
            long userId,
            Pageable pageable);

    default StudentsCardsPageModel buildPage(
            long userId,
            Pageable pageable) {

        return mode().toPage(loadStudents(userId, pageable));
    }
}