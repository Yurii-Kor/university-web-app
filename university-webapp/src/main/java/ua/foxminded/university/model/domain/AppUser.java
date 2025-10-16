package ua.foxminded.university.model.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import ua.foxminded.university.model.domain.enums.UserRole;
import ua.foxminded.university.model.domain.validation.RawPassword;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "app_user")
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	@ToString.Include
	private Long id;

	@NotBlank
	@Email
	@Size(max = 255)
	@ToString.Include
	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Size(min = 8, max = 100)
	@Pattern(
			regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\p{Punct}_])(?=\\S+$).{8,100}$", 
			message = "Password must be 8–100 chars, include upper/lowercase, digit, special, and contain no spaces",
			groups = RawPassword.class
			)
	@Column(nullable = false, length = 100)
	@Basic(fetch = FetchType.LAZY)
	private String password;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false, length = 16)
	private UserRole role;

	@NotBlank
	@Size(min = 2, max = 64)
	@Pattern(regexp = "^[A-Za-z]+(?:[-'][A-Za-z]+)*$")
	@ToString.Include
	@Column(nullable = false, length = 64)
	private String firstName;

	@NotBlank
	@Size(min = 2, max = 64)
	@Pattern(regexp = "^[A-Za-z]+(?:[-'][A-Za-z]+)*$")
	@ToString.Include
	@Column(nullable = false, length = 64)
	private String lastName;

	@Builder.Default
	@ToString.Include
	@Column(nullable = false)
	private boolean enabled = true;

	@Column(nullable = false, updatable = false, insertable = false)
	private OffsetDateTime createdAt;

	@ToString.Include(name = "createdAt")
	public String getCreatedAtIso() {
		return createdAt != null ? DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(createdAt) : null;
	}

	@PrePersist	@PreUpdate
	void normalizeEmail() {
		if (email != null)
			email = email.toLowerCase();
	}
}
