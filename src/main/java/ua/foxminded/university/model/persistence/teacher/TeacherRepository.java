package ua.foxminded.university.model.persistence.teacher;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.persistence.teacher.projection.DeletedTeacherCardProjection;
import ua.foxminded.university.model.persistence.teacher.projection.TeacherRoleChangeCardView;
import ua.foxminded.university.model.persistence.teacher.projection.TeacherDirectoryCardView;
import ua.foxminded.university.model.persistence.teacher.projection.TeacherOptionView;
import ua.foxminded.university.model.persistence.teacher.projection.TeacherProfileView;
import ua.foxminded.university.model.persistence.teacher.projection.TeacherWithCoursesCount;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    boolean existsByOfficeIgnoreCase(String office);
    
    @Query("select count(c.id) from Course c where c.teacher.id = :teacherId")
    long countCoursesByTeacherId(@Param("teacherId") long teacherId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Teacher t where t.id = :id")
    int deleteProfileById(@Param("id") long id);
    
    @Query("select t.id from Teacher t where t.id in :ids")
    List<Long> findExistingIds(@Param("ids") Collection<Long> ids);
    
    @Query(
            value = """
                    select new ua.foxminded.university.model.persistence.teacher.projection.TeacherDirectoryCardView(
                        t.id,
                        u.email,
                        u.firstName,
                        u.lastName,
                        t.academicRank,
                        t.office
                    )
                    from Teacher t
                    join t.user u
                    where u.enabled = true
                    order by lower(u.lastName),
                             lower(u.firstName),
                             lower(u.email),
                             t.id
                    """,
            countQuery = """
                    select count(t)
                    from Teacher t
                    join t.user u
                    where u.enabled = true
                    """)
    Page<TeacherDirectoryCardView> findTeacherCardsForAdmin(
            Pageable pageable);

    @Query(
            value = """
                    select new ua.foxminded.university.model.persistence.teacher.projection.TeacherDirectoryCardView(
                        t.id,
                        u.email,
                        u.firstName,
                        u.lastName,
                        t.academicRank,
                        t.office
                    )
                    from Teacher t
                    join t.user u
                    where u.enabled = true
                      and t.id <> :teacherId
                    order by lower(u.lastName),
                             lower(u.firstName),
                             lower(u.email),
                             t.id
                    """,
            countQuery = """
                    select count(t)
                    from Teacher t
                    join t.user u
                    where u.enabled = true
                      and t.id <> :teacherId
                    """)
    Page<TeacherDirectoryCardView> findTeacherCardsForTeacher(
            @Param("teacherId") long teacherId,
            Pageable pageable);

    @Query(
            value = """
                    select new ua.foxminded.university.model.persistence.teacher.projection.TeacherDirectoryCardView(
                        t.id,
                        u.email,
                        u.firstName,
                        u.lastName,
                        t.academicRank,
                        t.office
                    )
                    from Teacher t
                    join t.user u
                    where u.enabled = true
                      and exists (
                          select 1
                          from Course c
                          join c.groups cg
                          where c.teacher.id = t.id
                            and cg.id = (
                                select s.group.id
                                from Student s
                                where s.id = :studentId
                            )
                      )
                    order by lower(u.lastName),
                             lower(u.firstName),
                             lower(u.email),
                             t.id
                    """,
            countQuery = """
                    select count(t)
                    from Teacher t
                    join t.user u
                    where u.enabled = true
                      and exists (
                          select 1
                          from Course c
                          join c.groups cg
                          where c.teacher.id = t.id
                            and cg.id = (
                                select s.group.id
                                from Student s
                                where s.id = :studentId
                            )
                      )
                    """)
    Page<TeacherDirectoryCardView> findTeacherCardsForStudent(
            @Param("studentId") long studentId,
            Pageable pageable);
    
    @Query("""
            select new ua.foxminded.university.model.persistence.teacher.projection.TeacherProfileView(
                u.email,
                u.firstName,
                u.lastName,
                u.createdAt,
                t.academicRank,
                t.office
            )
            from Teacher t
            join t.user u
            where t.id = :id
        """)
    Optional<TeacherProfileView> findTeacherProfileViewById(@Param("id") Long id);
    
    @Query("""
            select new ua.foxminded.university.model.persistence.teacher.projection.TeacherOptionView(
              t.id, u.firstName, u.lastName, u.email
            )
            from Teacher t
            join t.user u
            order by lower(u.lastName), lower(u.firstName), t.id
        """)
    List<TeacherOptionView> findTeacherOptions();
    
    @Query(value = """
                select new ua.foxminded.university.model.persistence.teacher.projection.TeacherRoleChangeCardView(
                    t.id,
                    u.email,
                    u.firstName,
                    u.lastName,
                    u.enabled,
                    u.createdAt,
                    t.academicRank,
                    t.office
                )
                from Teacher t
                join t.user u
                order by lower(u.lastName), lower(u.firstName), lower(u.email)
            """, countQuery = """
                select count(t)
                from Teacher t
            """)
    Page<TeacherRoleChangeCardView> findTeacherRoleChangeCardsAll(Pageable pageable);
    
    @Query(value = """
            select *
            from teacher
            where id = :id
              and deleted_at is not null
            """, nativeQuery = true)
    Optional<Teacher> findDeletedById(@Param("id") Long id);
    
    @Query(value = """
            select
                t.id as id,
                u.email as email,
                u.first_name as firstName,
                u.last_name as lastName,
                u.enabled as enabled,
                u.created_at as createdAt,
                t.deleted_at as deletedAt,
                t.academic_rank as academicRank,
                t.office as office
            from teacher t
            join app_user u on u.id = t.id
            where t.deleted_at is not null
            order by t.deleted_at desc
            """, countQuery = """
            select count(*)
            from teacher t
            join app_user u on u.id = t.id
            where t.deleted_at is not null
            """, nativeQuery = true)
    Page<DeletedTeacherCardProjection> findDeletedTeacherCards(Pageable pageable);
    
    @Query("""
            select new ua.foxminded.university.model.persistence.teacher.projection.TeacherWithCoursesCount(
                t,
                count(c.id)
            )
            from Teacher t
            left join Course c on c.teacher.id = t.id
            where t.id = :id
            group by t
            """)
    Optional<TeacherWithCoursesCount> findTeacherWithCoursesCountById(@Param("id") Long id);
}
