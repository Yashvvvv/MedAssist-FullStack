package com.medassist.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Permission name is required")
    @Size(max = 50, message = "Permission name cannot exceed 50 characters")
    @Column(unique = true, nullable = false)
    private String name;

    @Size(max = 200, message = "Description cannot exceed 200 characters")
    @Column(length = 200)
    private String description;

    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany(mappedBy = "permissions")
    private Set<Role> roles = new HashSet<>();

    /**
     * Custom constructor for basic permission creation.
     */
    public Permission(String name, String description) {
        this.name = name;
        this.description = description;
        this.roles = new HashSet<>();
    }
}
