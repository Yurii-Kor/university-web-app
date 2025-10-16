package ua.foxminded.university.model.domain;

import java.util.Set;
import java.util.LinkedHashSet;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import ua.foxminded.university.model.domain.enums.AcademicRank;
import ua.foxminded.university.model.domain.enums.UserRole;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "teacher")
public class Teacher {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotNull @Valid
    @MapsId
    @OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "id")
    private AppUser user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ToString.Include
    @Column(name = "academic_rank", nullable = false, length = 64)
    private AcademicRank academicRank;

    @NotBlank
    @Pattern(
    		regexp = "^[A-Z]-\\d{3}$", 
    		message = "Office must look like 'B-105' (Letter-3digits)"
    		)
    @ToString.Include
    @Column(name = "office", nullable = false, length = 64)
    private String office;

    @OneToMany(mappedBy = "teacher", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Course> courses = new LinkedHashSet<>();

    @AssertTrue(message = "Teacher.user.role must be TEACHER")
    private boolean isUserRoleTeacher() {
        return user != null && user.getRole() == UserRole.TEACHER;
    }
}

