package com.exammanager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "problems")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Problem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer problemNumber;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    private String contentType = "TEXT";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @OneToOne(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Answer answer;
}
