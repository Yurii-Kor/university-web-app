package ua.foxminded.university.model.persistence.student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.persistence.student.projection.DeletedStudentCardProjection;
import ua.foxminded.university.model.persistence.student.projection.StudentCardView;
import ua.foxminded.university.model.persistence.student.projection.StudentProfileView;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
	
	@Query("select s.id from Student s where s.id in :ids")
    List<Long> findExistingIds(@Param("ids") Collection<Long> ids);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Student s set s.group = :newGroup where s.id in :ids")
    int updateGroupByIds(@Param("newGroup") StudyGroup newGroup,
                         @Param("ids") Collection<Long> ids);
	
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from Student s where s.id = :id")
	int deleteProfileById(@Param("id") long id);
	
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
	
	@Query("select s.group.id from Student s where s.id = :studentId")
	Optional<Long> findGroupIdByStudentId(@Param("studentId") Long studentId);
	
    @Query(value = """
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
            """, countQuery = """
                select count(s)
                from Student s
            """)
    Page<StudentCardView> findStudentCardsAll(Pageable pageable);
    
    @Query(value = """
            select s.group_id
            from student s
            join groups g on g.id = s.group_id
            where s.id = :id
              and s.deleted_at is not null
              and g.deleted_at is null
            """, nativeQuery = true)
    Optional<Long> findRestorableDeletedStudentGroupIdById(@Param("id") Long id);
    
    @Query(value = """
            select s.*
            from student s
            join groups g on g.id = s.group_id
            where s.id = :id
              and s.deleted_at is not null
              and g.deleted_at is null
            """, nativeQuery = true)
    Optional<Student> findRestorableDeletedById(@Param("id") Long id);
    
    @Query(value = """
            select
                s.id as id,
                u.email as email,
                u.first_name as firstName,
                u.last_name as lastName,
                u.enabled as enabled,
                u.created_at as createdAt,
                s.deleted_at as deletedAt,
                s.enrollment_year as enrollmentYear,
                g.name as groupName
            from student s
            join app_user u on u.id = s.id
            join groups g on g.id = s.group_id
            where s.deleted_at is not null
            order by s.deleted_at desc
            """, countQuery = """
            select count(*)
            from student s
            join app_user u on u.id = s.id
            join groups g on g.id = s.group_id
            where s.deleted_at is not null
            """, nativeQuery = true)
    Page<DeletedStudentCardProjection> findDeletedStudentCards(Pageable pageable);
}
