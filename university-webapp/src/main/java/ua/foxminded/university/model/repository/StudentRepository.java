package ua.foxminded.university.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ua.foxminded.university.model.domain.Student;
import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.repository.dto.StudentProfileView;

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
	
	@Query("""
			select new ua.foxminded.university.model.repository.dto.StudentProfileView(
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
}
