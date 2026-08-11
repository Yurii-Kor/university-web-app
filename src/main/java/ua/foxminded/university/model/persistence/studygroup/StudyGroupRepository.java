package ua.foxminded.university.model.persistence.studygroup;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ua.foxminded.university.model.domain.StudyGroup;
import ua.foxminded.university.model.persistence.studygroup.projection.GroupCardView;
import ua.foxminded.university.model.persistence.studygroup.projection.GroupView;
import ua.foxminded.university.model.persistence.studygroup.projection.StudentGroupHeaderView;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {

	boolean existsByNameIgnoreCase(String name);
	
	@Query("select g.id from StudyGroup g where g.id in :ids")
    List<Long> findExistingIds(@Param("ids") Collection<Long> ids);

	@Query("""
			select count(s.id)
			from StudyGroup g
			left join g.students s
			where g.id = :groupId
			""")
	long countStudentsByGroupId(@Param("groupId") Long groupId);

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
			select new ua.foxminded.university.model.persistence.studygroup.projection.GroupView(
				g.id,
				g.name
			)
			from Course c
			join c.groups g
			where c.id = :courseId
			order by g.name
			""")
	List<GroupView> findAssignedGroupOptions(@Param("courseId") Long courseId);

	@Query("""
			select new ua.foxminded.university.model.persistence.studygroup.projection.GroupView(
				g.id,
				g.name
			)
			from StudyGroup g
			where not exists (
				select 1
				from Course c
				join c.groups g2
				where c.id = :courseId
					and g2.id = g.id
			)
			order by g.name
			""")
	List<GroupView> findAvailableGroupOptions(@Param("courseId") Long courseId);
	
	@Query(
	        value = """
	                select new ua.foxminded.university.model.persistence.studygroup.projection.GroupCardView(
	                    g.id,
	                    g.name,
	                    count(distinct s.id),
	                    count(distinct c.id)
	                )
	                from StudyGroup g
	                left join g.students s
	                left join g.courses c
	                group by g.id, g.name
	                order by lower(g.name), g.id
	                """,
	        countQuery = """
	                select count(g)
	                from StudyGroup g
	                """
	    )
	Page<GroupCardView> findGroupCardsAll(Pageable pageable);
	
	@Query(
	        value = """
	                select new ua.foxminded.university.model.persistence.studygroup.projection.GroupCardView(
	                    g.id,
	                    g.name,
	                    count(distinct s.id),
	                    count(distinct c.id)
	                )
	                from Course c
	                join c.groups g
	                left join g.students s
	                where c.teacher.id = :teacherId
	                group by g.id, g.name
	                order by lower(g.name), g.id
	                """,
	        countQuery = """
	                select count(distinct g.id)
	                from Course c
	                join c.groups g
	                where c.teacher.id = :teacherId
	                """
	    )
	Page<GroupCardView> findGroupCardsByTeacherId(
	            @Param("teacherId") Long teacherId,
	            Pageable pageable);
	
	@Query("""
	        select new ua.foxminded.university.model.persistence.studygroup.projection.StudentGroupHeaderView(
	            g.id,
	            g.name,
	            count(distinct groupStudent.id)
	        )
	        from Student currentStudent
	        join currentStudent.group g
	        left join g.students groupStudent
	        where currentStudent.id = :studentId
	        group by g.id, g.name
	        """)
	Optional<StudentGroupHeaderView> findStudentGroupHeader(
	        @Param("studentId") Long studentId);
}