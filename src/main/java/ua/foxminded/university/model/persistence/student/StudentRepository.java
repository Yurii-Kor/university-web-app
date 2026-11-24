package ua.foxminded.university.model.persistence.student;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.persistence.student.projection.AdminStudentCardView;
import ua.foxminded.university.model.persistence.student.projection.StudentCardView;
import ua.foxminded.university.model.persistence.student.projection.StudentPeerCardView;
import ua.foxminded.university.model.persistence.student.projection.StudentProfileView;
import ua.foxminded.university.model.persistence.student.projection.TeacherStudentCardView;

public interface StudentRepository extends JpaRepository<Student, Long> {

	@Query("""
			select new ua.foxminded.university.model.persistence.student.projection.StudentProfileView(
			    u.email,
			    u.firstName,
			    u.lastName,
			    u.createdAt,
			    s.enrollmentYear,
			    g.name
			)
			from Student s
			join s.user u
			join s.group g
			where s.id = :id
			""")
    Optional<StudentProfileView> findStudentProfileViewById(@Param("id") Long id);

	@Query("""
			select s.group.id
			from Student s
			where s.id = :studentId
			""")
    Optional<Long> findGroupIdByStudentId(@Param("studentId") Long studentId);

	@Query(
			value = """
					select new ua.foxminded.university.model.persistence.student.projection.StudentCardView(
					    s.id,
					    u.email,
					    u.firstName,
					    u.lastName,
					    u.enabled,
					    u.createdAt,
					    s.enrollmentYear,
					    g.name
					)
					from Student s
					join s.user u
					join s.group g
					order by lower(u.lastName), lower(u.firstName), lower(u.email)
					""",
			countQuery = """
					select count(s)
					from Student s
					""")
    Page<StudentCardView> findStudentCardsForAccountManagement(Pageable pageable);
	
	@Query(
	        value = """
	                select new ua.foxminded.university.model.persistence.student.projection.AdminStudentCardView(
	                    s.id,
	                    u.email,
	                    u.firstName,
	                    u.lastName,
	                    g.id,
	                    g.name,
	                    s.enrollmentYear
	                )
	                from Student s
	                join s.user u
	                join s.group g
	                where u.enabled = true
	                order by lower(u.lastName),
	                         lower(u.firstName),
	                         lower(u.email),
	                         s.id
	                """,
	        countQuery = """
	                select count(s)
	                from Student s
	                join s.user u
	                join s.group g
	                where u.enabled = true
	                """)
    Page<AdminStudentCardView> findStudentCardsForAdmin(Pageable pageable);

	@Query(
	        value = """
	                select new ua.foxminded.university.model.persistence.student.projection.TeacherStudentCardView(
	                    s.id,
	                    u.email,
	                    u.firstName,
	                    u.lastName,
	                    g.name
	                )
	                from Student s
	                join s.user u
	                join s.group g
	                where u.enabled = true
	                  and exists (
	                      select 1
	                      from Course c
	                      join c.groups cg
	                      where c.teacher.id = :teacherId
	                        and cg.id = g.id
	                  )
	                order by lower(g.name),
	                         lower(u.lastName),
	                         lower(u.firstName),
	                         lower(u.email),
	                         s.id
	                """,
	        countQuery = """
	                select count(s)
	                from Student s
	                join s.user u
	                join s.group g
	                where u.enabled = true
	                  and exists (
	                      select 1
	                      from Course c
	                      join c.groups cg
	                      where c.teacher.id = :teacherId
	                        and cg.id = g.id
	                  )
	                """)
	Page<TeacherStudentCardView> findStudentCardsForTeacher(
	        @Param("teacherId") long teacherId,
	        Pageable pageable);

	@Query(
	        value = """
	                select new ua.foxminded.university.model.persistence.student.projection.StudentPeerCardView(
	                    s.id,
	                    u.email,
	                    u.firstName,
	                    u.lastName
	                )
	                from Student s
	                join s.user u
	                where u.enabled = true
	                  and s.id <> :studentId
	                  and s.group.id = (
	                      select currentStudent.group.id
	                      from Student currentStudent
	                      where currentStudent.id = :studentId
	                  )
	                order by lower(u.lastName),
	                         lower(u.firstName),
	                         lower(u.email),
	                         s.id
	                """,
	        countQuery = """
	                select count(s)
	                from Student s
	                join s.user u
	                where u.enabled = true
	                  and s.id <> :studentId
	                  and s.group.id = (
	                      select currentStudent.group.id
	                      from Student currentStudent
	                      where currentStudent.id = :studentId
	                  )
	                """)
	Page<StudentPeerCardView> findStudentCardsForStudent(
	        @Param("studentId") long studentId,
	        Pageable pageable);

	@Query(
			value = """
					select s.group_id
					from student s
					join groups g on g.id = s.group_id
					where s.id = :id
					  and s.deleted_at is not null
					  and g.deleted_at is null
					""",
			nativeQuery = true)
    Optional<Long> findRestorableDeletedStudentGroupIdById(@Param("id") Long id);

	@Query(
			value = """
					select s.*
					from student s
					join groups g on g.id = s.group_id
					where s.id = :id
					  and s.deleted_at is not null
					  and g.deleted_at is null
					""",
			nativeQuery = true)
    Optional<Student> findRestorableDeletedById(@Param("id") Long id);
}
