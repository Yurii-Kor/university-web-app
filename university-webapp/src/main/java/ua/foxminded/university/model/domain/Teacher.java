package ua.foxminded.university.model.domain;

import java.util.Set;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;

import jakarta.persistence.*;
import lombok.*;

import ua.foxminded.university.model.domain.enums.AcademicRank;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "teacher")
@SQLDelete(sql = "update teacher set deleted_at = now() where id = ? and deleted_at is null")
@SQLRestriction("deleted_at is null")
public class Teacher {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @MapsId
    @OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "id")
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @ToString.Include
    @Column()
    private AcademicRank academicRank;

    @ToString.Include
    @Column()
    private String office;

    @OneToMany(mappedBy = "teacher", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Course> courses = new LinkedHashSet<>();
    
    @Column()
    private OffsetDateTime deletedAt;
}
