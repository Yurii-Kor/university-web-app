package ua.foxminded.university.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.repository.dto.GroupStudentAgg;

import java.util.Collection;
import java.util.List;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {

	@Query("select g.id from StudyGroup g where g.id in :ids")
	List<Long> findExistingIds(@Param("ids") Collection<Long> ids);

	@Query("""
				select new ua.foxminded.university.model.repository.dto.GroupStudentAgg(g.id, count(s.id))
				from StudyGroup g
				left join g.students s
				where g.id in :ids
				group by g.id
			""")
	List<GroupStudentAgg> countStudentsByGroupIds(@Param("ids") Collection<Long> ids);

	@Query("""
				select lower(g.name)
				from StudyGroup g
				where lower(g.name) in :names
			""")
	List<String> findExistingNamesIgnoreCase(@Param("names") Collection<String> names);

	@Query("""
			    select lower(g.name)
			    from StudyGroup g
			    where lower(g.name) in :names
			      and g.id not in :ids
			""")
	List<String> findConflictingNamesIgnoreCase(@Param("names") Collection<String> names,
			@Param("ids") Collection<Long> ids);
}
