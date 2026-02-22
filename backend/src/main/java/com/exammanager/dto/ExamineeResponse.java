package com.exammanager.dto;

import com.exammanager.entity.Examinee;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamineeResponse {
    private Long id;
    private String name;
    private LocalDate birthDate;

    public static ExamineeResponse from(Examinee examinee) {
        return ExamineeResponse.builder()
                .id(examinee.getId())
                .name(examinee.getName())
                .birthDate(examinee.getBirthDate())
                .build();
    }
}
