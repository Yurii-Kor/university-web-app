package ua.foxminded.university.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.foxminded.university.model.domain.Course;

import java.util.Collection;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
	boolean existsByIdAndTeacher_Id(Long courseId, Long teacherId);
	boolean existsByIdAndGroups_Id(Long courseId, Long groupId);

	@Query("select c.id from Course c where c.id in :ids")
	List<Long> findExistingIds(@Param("ids") Collection<Long> ids);

	@Query("""
			    select lower(c.code)
			    from Course c
			    where lower(c.code) in :codes
			""")
	List<String> findExistingCodesIgnoreCase(@Param("codes") Collection<String> codes);

	@Query("""
			    select lower(c.code)
			    from Course c
			    where lower(c.code) in :codes
			      and c.id not in :ids
			""")
	List<String> findConflictingCodesIgnoreCase(@Param("codes") Collection<String> codes,
			@Param("ids") Collection<Long> ids);

	@Query("""
			    select lower(c.name)
			    from Course c
			    where lower(c.name) in :names
			""")
	List<String> findExistingNamesIgnoreCase(@Param("names") Collection<String> names);
}
