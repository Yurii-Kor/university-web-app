package ua.foxminded.university.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.foxminded.university.model.domain.Course;
import ua.foxminded.university.model.repository.dto.CourseCardView;
import ua.foxminded.university.model.repository.dto.CourseHeaderView;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
	
	@Query("""
		    select new ua.foxminded.university.model.repository.dto.CourseCardView(
		        c.id,
		        c.code,
		        c.name,
		        c.description,
		        t.id,
		        u.email,
		        u.firstName,
		        u.lastName,
		        count(distinct g.id)
		    )
		    from Course c
		    join c.teacher t
		    join t.user u
		    left join c.groups g
		    group by c.id, c.code, c.name, c.description, t.id, u.email, u.firstName, u.lastName
		    order by lower(c.code)
		""")
	List<CourseCardView> findCourseCardsAll();
	
	@Query("""
		    select new ua.foxminded.university.model.repository.dto.CourseCardView(
		        c.id,
		        c.code,
		        c.name,
		        c.description,
		        t.id,
		        u.email,
		        u.firstName,
		        u.lastName,
		        count(distinct g.id)
		    )
		    from Course c
		    join c.teacher t
		    join t.user u
		    left join c.groups g
		    where t.id = :teacherId
		    group by c.id, c.code, c.name, c.description, t.id, u.email, u.firstName, u.lastName
		    order by lower(c.code)
		""")
	List<CourseCardView> findCourseCardsByTeacherId(@Param("teacherId") Long teacherId);

	@Query("""
		    select new ua.foxminded.university.model.repository.dto.CourseCardView(
		        c.id,
		        c.code,
		        c.name,
		        c.description,
		        t.id,
		        u.email,
		        u.firstName,
		        u.lastName,
		        count(distinct gAll.id)
		    )
		    from Course c
		    join c.teacher t
		    join t.user u

		    join c.groups sg
		    join Student s on s.group = sg

		    left join c.groups gAll

		    where s.id = :studentId
		    group by c.id, c.code, c.name, c.description, t.id, u.email, u.firstName, u.lastName
		    order by lower(c.code)
		""")
	List<CourseCardView> findCourseCardsByGroupId(@Param("studentId") Long studentId);
	
	@Query("""
	        select new ua.foxminded.university.model.repository.dto.CourseHeaderView(c.id, c.code, c.name)
	        from Course c
	        where c.id = :id
	    """)
	Optional<CourseHeaderView> findCourseHeaderById(@Param("id") Long id);
}
