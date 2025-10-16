package ua.foxminded.university.model.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "groups")
public class StudyGroup {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	@ToString.Include
	private Long id;

	@NotBlank
	@Size(max = 255)
	@Pattern(
			regexp = "^[A-Z]{2}-\\d{3}$", 
			message = "Group name must look like 'CS-101'"
			)
	@Column(name = "name", nullable = false, length = 255)
	@ToString.Include
	private String name;

	@Builder.Default
	@OneToMany(mappedBy = "group")
	private List<Student> students = new ArrayList<>();

	@Builder.Default
	@ManyToMany(mappedBy = "groups")
	private Set<Course> courses = new LinkedHashSet<>();
}
