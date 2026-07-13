package ua.foxminded.university.model.persistence.studygroup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.persistence.studygroup.projection.GroupView;
import ua.foxminded.university.model.persistence.studygroup.projection.IdCountAgg;

import java.util.Collection;
import java.util.List;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {

	@Query("select g.id from StudyGroup g where g.id in :ids")
	List<Long> findExistingIds(@Param("ids") Collection<Long> ids);
	
	@Query("""
	        select new ua.foxminded.university.model.persistence.studygroup.projection.GroupView(
	            g.id,
	            g.name
	        )
	        from StudyGroup g
	        order by lower(g.name), g.id
	    """)
	List<GroupView> findGroupOptions();

	@Query("""
				select new ua.foxminded.university.model.persistence.studygroup.projection.IdCountAgg(g.id, count(s.id))
				from StudyGroup g
				left join g.students s
				where g.id in :ids
				group by g.id
			""")
	List<IdCountAgg> countStudentsByGroupIds(@Param("ids") Collection<Long> ids);

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
	
	@Query("""
	        select new ua.foxminded.university.model.persistence.studygroup.projection.GroupView(g.id, g.name)
	        from Course c
	        join c.groups g
	        where c.id = :courseId
	        order by g.name
	    """)
	List<GroupView> findAssignedGroupOptions(@Param("courseId") Long courseId);

    @Query("""
	        select new ua.foxminded.university.model.persistence.studygroup.projection.GroupView(g.id, g.name)
	        from StudyGroup g
	        where not exists (
	            select 1
	            from Course c
	            join c.groups g2
	            where c.id = :courseId and g2.id = g.id
	        )
	        order by g.name
	    """)
	List<GroupView> findAvailableGroupOptions(@Param("courseId") Long courseId);
	    
    @Query(value = """
                select new ua.foxminded.university.model.persistence.studygroup.projection.GroupView(
                    g.id,
                    g.name
                )
                from StudyGroup g
                order by lower(g.name)
            """, countQuery = """
                select count(g)
                from StudyGroup g
            """)
    Page<GroupView> findGroupCardsAll(Pageable pageable);
}
