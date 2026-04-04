package ua.foxminded.university.model.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.foxminded.university.model.domain.Course;
import ua.foxminded.university.model.repository.dto.CourseCardView;
import ua.foxminded.university.model.repository.dto.CourseHeaderView;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsByIdAndTeacher_Id(Long courseId, Long teacherId);

    boolean existsByIdAndGroups_Id(Long courseId, Long groupId);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByNameIgnoreCase(String name);

    @Query(
        value = """
            select new ua.foxminded.university.model.repository.dto.CourseCardView(
                c.id,
                c.code,
                c.name,
                c.description,
                t.id,
                u.email,
                u.firstName,
                u.lastName
            )
            from Course c
            join c.teacher t
            join t.user u
            order by lower(c.code)
        """,
        countQuery = """
            select count(c)
            from Course c
        """
    )
    Page<CourseCardView> findCourseCardsAll(Pageable pageable);

    @Query(
        value = """
            select new ua.foxminded.university.model.repository.dto.CourseCardView(
                c.id,
                c.code,
                c.name,
                c.description,
                t.id,
                u.email,
                u.firstName,
                u.lastName
            )
            from Course c
            join c.teacher t
            join t.user u
            where t.id = :teacherId
            order by lower(c.code)
        """,
        countQuery = """
            select count(c)
            from Course c
            join c.teacher t
            where t.id = :teacherId
        """
    )
    Page<CourseCardView> findCourseCardsByTeacherId(@Param("teacherId") Long teacherId, Pageable pageable);

    @Query(
        value = """
            select new ua.foxminded.university.model.repository.dto.CourseCardView(
                c.id,
                c.code,
                c.name,
                c.description,
                t.id,
                u.email,
                u.firstName,
                u.lastName
            )
            from Course c
            join c.teacher t
            join t.user u
            join c.groups sg
            join Student s on s.group = sg
            where s.id = :studentId
            order by lower(c.code)
        """,
        countQuery = """
            select count(distinct c)
            from Course c
            join c.groups sg
            join Student s on s.group = sg
            where s.id = :studentId
        """
    )
    Page<CourseCardView> findCourseCardsByStudentId(@Param("studentId") Long studentId, Pageable pageable);

    @Query("""
            select new ua.foxminded.university.model.repository.dto.CourseHeaderView(c.id, c.code, c.name)
            from Course c
            where c.id = :id
        """)
    Optional<CourseHeaderView> findCourseHeaderById(@Param("id") Long id);
}