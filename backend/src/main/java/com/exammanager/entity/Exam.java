package com.exammanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exams")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String problemFileName;

    private String answerFileName;

    @Builder.Default
    private Boolean deleted = false;

    @Builder.Default
    private Boolean active = false;

    private Integer timeLimit;

    @Builder.Default
    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Problem> problems = new ArrayList<>();

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
