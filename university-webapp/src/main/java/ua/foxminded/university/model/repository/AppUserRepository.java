package ua.foxminded.university.model.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import ua.foxminded.university.model.domain.AppUser;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.repository.dto.AdminProfileView;
import ua.foxminded.university.model.repository.dto.AdminRowView;

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
			select new ua.foxminded.university.model.repository.dto.AdminProfileView(
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
	
	@Query("""
		    select new ua.foxminded.university.model.repository.dto.AdminRowView(
		        u.id,
		        u.email,
		        u.firstName,
		        u.lastName,
		        u.createdAt,
		        u.enabled
		    )
		    from AppUser u
		    where u.role = ua.foxminded.university.model.domain.enums.UserRole.ADMIN
		      order by u.createdAt desc
		""")
	List<AdminRowView> findAdminRows();

}
