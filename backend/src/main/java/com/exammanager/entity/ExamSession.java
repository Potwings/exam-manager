package com.exammanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_sessions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"examinee_id", "exam_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "examinee_id", nullable = false)
    private Examinee examinee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    private LocalDateTime startedAt;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
    }
}
