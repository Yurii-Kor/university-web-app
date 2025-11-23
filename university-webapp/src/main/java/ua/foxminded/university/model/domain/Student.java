package ua.foxminded.university.model.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "student")
public class Student {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @MapsId
    @OneToOne(optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "id")
    private AppUser user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private StudyGroup group;

    @ToString.Include
    @Column()
    private Integer enrollmentYear;
}

