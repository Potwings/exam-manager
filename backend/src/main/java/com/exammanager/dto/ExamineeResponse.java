package com.exammanager.dto;

import com.exammanager.entity.Examinee;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamineeResponse {
    private Long id;
    private String name;

    public static ExamineeResponse from(Examinee examinee) {
        return ExamineeResponse.builder()
                .id(examinee.getId())
                .name(examinee.getName())
                .build();
    }
}
