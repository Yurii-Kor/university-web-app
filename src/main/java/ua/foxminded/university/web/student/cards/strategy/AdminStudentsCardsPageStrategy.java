package ua.foxminded.university.web.student.cards.strategy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import ua.foxminded.university.model.persistence.student.projection.AdminStudentCardView;
import ua.foxminded.university.service.student.StudentService;
import ua.foxminded.university.web.student.cards.StudentsCardsPageMode;
import ua.foxminded.university.web.student.cards.StudentsCardsPageStrategy;

@Component
@RequiredArgsConstructor
public class AdminStudentsCardsPageStrategy
        implements StudentsCardsPageStrategy<AdminStudentCardView> {

    private final StudentService studentService;

    @Override
    public StudentsCardsPageMode mode() {
        return StudentsCardsPageMode.ADMIN;
    }

    @Override
    public Page<AdminStudentCardView> loadStudents(
            long userId,
            Pageable pageable) {

        return studentService.listStudentCardsForAdmin(
                pageable);
    }
}
