package ua.foxminded.university.model.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import ua.foxminded.university.model.domain.Teacher;
import ua.foxminded.university.model.repository.dto.TeacherCardView;
import ua.foxminded.university.model.repository.dto.IdCountAgg;
import ua.foxminded.university.model.repository.dto.TeacherOptionView;
import ua.foxminded.university.model.repository.dto.TeacherProfileView;

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
	
	@Query("""
			    select new ua.foxminded.university.model.repository.dto.IdCountAgg(t.id, count(c.id))
			    from Teacher t
			    left join Course c on c.teacher.id = t.id
			    where t.id in :ids
			    group by t.id
			""")
	List<IdCountAgg> countCoursesByTeacherIds(@Param("ids") Collection<Long> ids);
	
	@Query("""
			select new ua.foxminded.university.model.repository.dto.TeacherProfileView(
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
			select new ua.foxminded.university.model.repository.dto.TeacherOptionView(
			  t.id, u.firstName, u.lastName, u.email
			)
			from Teacher t
			join t.user u
			order by lower(u.lastName), lower(u.firstName), t.id
		""")
	List<TeacherOptionView> findTeacherOptions();
	
    @Query(value = """
                select new ua.foxminded.university.model.repository.dto.TeacherCardView(
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
    Page<TeacherCardView> findTeacherCardsAll(Pageable pageable);
}
