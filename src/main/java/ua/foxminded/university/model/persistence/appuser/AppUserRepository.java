package ua.foxminded.university.model.persistence.appuser;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.persistence.appuser.projection.AdminProfileView;
import ua.foxminded.university.model.persistence.appuser.projection.AdminRowView;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
	
	Optional<AppUser> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);

	long countByRole(UserRole role);

	long countByRoleAndEnabled(UserRole role, boolean enabled);
	
	@Query("""
			select lower(u.email)
			from AppUser u
			where lower(u.email) in :emails
		""")
	List<String> findExistingEmailsIgnoreCase(@Param("emails") Collection<String> emails);
	
	@Query("""
			select new ua.foxminded.university.model.persistence.appuser.projection.AdminProfileView(
			    u.email,
			    u.firstName,
			    u.lastName,
			    u.createdAt
			)
			from AppUser u
			where u.id = :id
			  and u.role = ua.foxminded.university.model.domain.enums.UserRole.ADMIN
		""")
	Optional<AdminProfileView> findAdminProfileViewById(@Param("id") Long id);
	
	@Query(value = """
	        select new ua.foxminded.university.model.persistence.appuser.projection.AdminRowView(
	            u.id,
	            u.email,
	            u.firstName,
	            u.lastName,
	            u.createdAt,
	            u.enabled
	        )
	        from AppUser u
	        where u.role = ua.foxminded.university.model.domain.enums.UserRole.ADMIN
	        order by
	            case when u.id = :selfId then 0 else 1 end,
	            case when u.enabled = true then 0 else 1 end,
	            u.id asc
	        """,
	       countQuery = """
	        select count(u)
	        from AppUser u
	        where u.role = ua.foxminded.university.model.domain.enums.UserRole.ADMIN
	        """)
	Page<AdminRowView> findAdminRowsForView(@Param("selfId") Long selfId, Pageable pageable);

}
